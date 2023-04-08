SECTION "MAIN", ROM0[$0100]
    ; Write "Hello world"
    ld hl, $FF01
    ld [hl], 72
    ld [hl], 101
    ld [hl], 108
    ld [hl], 108
    ld [hl], 111
    ld [hl], 32
    ld [hl], 119
    ld [hl], 111
    ld [hl], 114
    ld [hl], 108
    ld [hl], 100
    stop