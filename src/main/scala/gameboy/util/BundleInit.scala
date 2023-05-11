package gameboy.util

import chisel3._

object BundleInit {
  implicit class AddInitConstruct[T <: Record](bun: T) {
    def Init(fs: (T => (Data, Data))*): T = {
      chisel3.experimental.requireIsChiselType(bun, "Can only init from clean types")
      val init = Wire(bun)
      for (f <- fs) {
        val (field, value) = f(init)
        // TODO check that field is a field of init
        field := value
      }
      init
    }
  }
}