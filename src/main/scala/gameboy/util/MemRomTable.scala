package gameboy.util

import chisel3._
import chisel3.util._
import chisel3.experimental.{ChiselAnnotation, annotate}

/** A table stored in a memory-based ROM */
class MemRomTable[T <: Data](gen: T, contents: Seq[T]) extends Module {
  val addressWidth = log2Ceil(contents.length)
  val dataWidth = gen.getWidth
  val io = IO(new Bundle {
    val addr = Input(UInt(addressWidth.W))
    val data = Output(gen)
  })

  val mem = Mem(contents.length, UInt(dataWidth.W))
  annotate(new ChiselAnnotation {
    // XXX: This only works with firrtl. I can't find an equivalent for circt.
    override def toFirrtl = firrtl.annotations.MemoryArrayInitAnnotation(
      mem.toTarget, contents.map(x => x.litValue)
    )
  })
  suppressEnumCastWarning {
    io.data := mem.read(io.addr).asTypeOf(gen)
  }
}
