package gameboy.util

import chisel3._
import chisel3.util._
import chisel3.experimental.{ChiselAnnotation, annotate}
import gameboy.Gameboy

/** A table stored in a memory-based ROM */
class MemRomTable[T <: Data](config: Gameboy.Configuration, gen: T, contents: Seq[T]) extends Module {
  val addressWidth = log2Ceil(contents.length)
  val dataWidth = gen.getWidth
  val io = IO(new Bundle {
    val addr = Input(UInt(addressWidth.W))
    val data = Output(gen)
  })


  if (config.optimizeForSimulation) {
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
  } else {
    val table = VecInit(contents)
    io.data := table(io.addr)
  }
}
