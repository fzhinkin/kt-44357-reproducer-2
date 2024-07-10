package test

import kotlinx.cinterop.*
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.posix.memcpy
import kotlin.system.measureTimeMicros
import kotlin.test.Test
import kotlin.test.assertEquals

// @SymbolName("strlen")
// external fun strlen(ptr: CPointer<ByteVar>): ULong

// The test was taken from https://youtrack.jetbrains.com/issue/KT-44357
abstract class BenchBase(val original: String) {
    val rawBytes = original.encodeToByteArray()
    val originalBytes = rawBytes.copyOf(rawBytes.size + 1) /* add a NULL byte at the end */
    val range = 1..100000

    // MARK: Part 1 - CPointer.toKStringFromUtf8() v.s. converting through NSString

    @Test
    fun measureToKString() = memScoped {
        val ptr = originalBytes.toCValues().ptr

        var avg: Double = 0.0
        for (itr in range) {
            var roundtrippedValue: String? = null
            val time = measureTimeMicros {
                roundtrippedValue = ptr.toKStringFromUtf8()
            }
            assertEquals(original, roundtrippedValue!!)
            avg += time.toDouble() / range.endInclusive
        }

        println("@@@ toKString $avg us")
    }

    @Test
    fun measureCopyingValidatingNSString() = memScoped {
        val ptr = originalBytes.toCValues().ptr

        var avg: Double = 0.0
        for (itr in range) {
            var roundtrippedValue: String? = null
            val time = measureTimeMicros {
                roundtrippedValue = NSString
                    .create(uTF8String = ptr)
                    .toString()
            }
            assertEquals(original, roundtrippedValue!!)
            avg += time.toDouble() / range.endInclusive
        }

        println("@@@ CopyingValidatingNSString $avg us")
    }

    // MARK: Part 2 - Converting via ByteArray, and ByteArray UTF8 decoding performance
/*
    @Test
    fun measureStrlenMemcpyThenDecodeByteArray() = memScoped {
        val ptr = originalBytes.toCValues().ptr
        var avg: Double = 0.0

        for (itr in range) {
            var roundtrippedValue: String? = null
            val time = measureTimeMicros {
                val count = strlen(ptr)
                ByteArray(count.toInt()).usePinned {
                    memcpy(it.addressOf(0), ptr, count.toULong())
                    roundtrippedValue = it.get().decodeToString()
                }
            }
            assertEquals(original, roundtrippedValue!!)
            avg += time.toDouble() / range.endInclusive
        }
        println("@@@ StrlenMemcpyThenDecodeByteArray $avg us")
    }
*/
    @Test
    fun measureCopyToByteArrayThenDecodeByteArray() = memScoped {
        val ptr = originalBytes.toCValues().ptr
        var avg: Double = 0.0
        val count = originalBytes.size - 1

        for (itr in range) {
            var roundtrippedValue: String? = null
            val time = measureTimeMicros {
                ByteArray(count).usePinned {
                    memcpy(it.addressOf(0), ptr, count.toULong())
                    roundtrippedValue = it.get().decodeToString()
                }
            }
            assertEquals(original, roundtrippedValue!!)
            avg += time.toDouble() / range.endInclusive
        }
        println("@@@ CopyToByteArrayThenDecodeByteArray $avg us")
    }

    @Test
    fun measureJustDecodeByteArray() = memScoped {
        var avg: Double = 0.0
        val count = originalBytes.size - 1

        for (itr in range) {
            var roundtrippedValue: String? = null
            val time = measureTimeMicros {
                roundtrippedValue = originalBytes.decodeToString(0, count)
            }
            assertEquals(original, roundtrippedValue!!)
            avg += time.toDouble() / range.endInclusive
        }
        println("@@@ JustDecodeByteArray $avg us")
    }
}
class AsciiBench : BenchBase("well-formed-utf8".repeat(128) /* 2048 bytes */)
// "😘️😂😀😝".repeat(128) does not work properly as it is, for some reason, so the string decoded from raw utf8
class Utf8Bench : BenchBase(
    byteArrayOf(-16, -97, -104, -104, -17, -72, -113, -16, -97, -104, -126, -16, -97, -104, -128, -16, -97, -104, -99)
        .decodeToString().repeat(128) /* 19 bytes * 128 = 2432 bytes */)
