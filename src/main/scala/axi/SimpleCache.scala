package axi

import chisel3._
import chisel3.util._

class CacheEntry(tagWidth: Int, dataWidth: Int) extends Bundle {
  val tag = UInt(tagWidth.W)
  val data = UInt(dataWidth.W)
}

/**
 * A simple byte-addressed direct-mapped cache for use with AxiLiteInitiator.
 * Caches reads only.
 *
 * Block offset bits: log2(dataWidth / 8)
 * Index bits: given
 * Tag bits: remaining
 */
class SimpleCache(addrWidth: Int, dataWidth: Int, indexWidth: Int) extends Module {
  val io = IO(new AxiLiteInitiatorIo(addrWidth, dataWidth) {
    val cacheInvalidate = Input(Bool())

    val statHits = Output(UInt(32.W))
    val statMisses = Output(UInt(32.W))
  })
  val offsetWidth = log2Ceil(dataWidth / 8)
  val tagWidth = addrWidth - indexWidth - offsetWidth
  val entries = 1 << indexWidth

  // Stats -- reads only
  val statHits = RegInit(0.U(32.W))
  val statMisses = RegInit(0.U(32.W))
  io.statHits := statHits
  io.statMisses := statMisses

  // Cache
  val cache = RegInit(VecInit(Seq.fill(entries)(0.U.asTypeOf(new CacheEntry(tagWidth, dataWidth)))))
  val cacheValid = RegInit(VecInit(Seq.fill(entries)(false.B)))
  when (io.cacheInvalidate) {
    cacheValid := VecInit(Seq.fill(entries)(false.B))
    statHits := 0.U
    statMisses := 0.U
  }

  // Initiator
  val initiator = Module(new AxiLiteInitiator(addrWidth, dataWidth))
  io.signals <> initiator.io.signals
  initiator.io.enable := false.B
  initiator.io.address := io.address
  initiator.io.read := io.read
  initiator.io.writeData := io.writeData
  initiator.io.writeStrobe := io.writeStrobe

  // Address decode
  val addrTag = io.address(addrWidth - 1, offsetWidth + indexWidth)
  val addrIndex = io.address(offsetWidth + indexWidth - 1, offsetWidth)
  val cacheHit = cacheValid(addrIndex) && cache(addrIndex).tag === addrTag

  val busy = RegInit(false.B)
  val readData = Reg(UInt(dataWidth.W))
  io.busy := busy
  io.readData := readData
  val pendingRead = Reg(Bool())
  val pendingTag = Reg(UInt(tagWidth.W))
  val pendingIndex = Reg(UInt(indexWidth.W))

  when (busy && !initiator.io.busy) {
    busy := false.B

    when (pendingRead) {
      readData := initiator.io.readData
      cache(pendingIndex).tag := pendingTag
      cache(pendingIndex).data := initiator.io.readData
      cacheValid(pendingIndex) := true.B
    }
  }

  when (io.enable && !busy) {
    when (io.read) {
      when (cacheHit) {
        busy := false.B
        readData := cache(addrIndex).data
        statHits := statHits + 1.U
      } .otherwise {
        busy := true.B
        initiator.io.enable := true.B
        pendingRead := true.B
        pendingTag := addrTag
        pendingIndex := addrIndex
        statMisses := statMisses + 1.U
      }
    } .otherwise {
      when (cacheHit) {
        // Simple approach: invalidate cache entry on write
        cacheValid(addrIndex) := false.B
      }

      busy := true.B
      initiator.io.enable := true.B
      pendingRead := false.B
    }
  }
}
