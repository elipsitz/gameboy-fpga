package axi

import chisel3._
import chisel3.util._

class CacheEntry(tagWidth: Int, dataWidth: Int) extends Bundle {
  val tag = UInt(tagWidth.W)
  val data = UInt(dataWidth.W)
}

object SimpleCacheState extends ChiselEnum {
  val idle, waitCache, waitMem = Value
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
  val state = RegInit(SimpleCacheState.idle)
  val cache = SyncReadMem(entries, new CacheEntry(tagWidth, dataWidth))
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

  // Cache access
  val cacheEnable = WireDefault(false.B)
  val cacheEntry = cache.read(addrIndex, cacheEnable)
  val cacheHit = cacheValid(addrIndex) && cacheEntry.tag === addrTag

  val readData = Reg(UInt(dataWidth.W))
  io.busy := state =/= SimpleCacheState.idle
  io.readData := readData
  val pendingRead = Reg(Bool())
  val pendingTag = Reg(UInt(tagWidth.W))
  val pendingIndex = Reg(UInt(indexWidth.W))

  switch (state) {
    is (SimpleCacheState.idle) {
      when (io.enable) {
        // TODO if not valid just skip to waitMem?
        state := SimpleCacheState.waitCache
        cacheEnable := true.B
        pendingRead := io.read
        pendingTag := addrTag
        pendingIndex := addrIndex
      }
    }

    is (SimpleCacheState.waitCache) {
      when (pendingRead) {
        when (cacheHit) {
          state := SimpleCacheState.idle
          readData := cacheEntry.data
          statHits := statHits + 1.U
        } .otherwise {
          state := SimpleCacheState.waitMem
          initiator.io.enable := true.B
          statMisses := statMisses + 1.U
        }
      } .otherwise {
        when (cacheHit) {
          // Simple approach: invalidate cache entry on write
          // Note: since we don't need to read from the cache, we could have started this in the idle state.
          cacheValid(addrIndex) := false.B
        }

        state := SimpleCacheState.waitMem
        initiator.io.enable := true.B
      }
    }

    is (SimpleCacheState.waitMem) {
      when (!initiator.io.busy) {
        state := SimpleCacheState.idle

        when (pendingRead) {
          readData := initiator.io.readData
          val entry = Wire(new CacheEntry(tagWidth, dataWidth))
          entry.tag := pendingTag
          entry.data := initiator.io.readData
          cache.write(pendingIndex, entry)
          cacheValid(pendingIndex) := true.B
        }
      }
    }
  }
}
