SECTION "ROM0", ROM0
    ; Test load immediate instructions.
    ld a, $00
    ld b, $BB
    ld c, $CC
    ld de, $DDEE

    ; Test basic memory accesses.
    ld hl, $C000
    ld a, $2B
    ld [hl], a

    ; Test jump.
    jp skip1
    halt
skip1:

    ; Test compare and jump.
    ld a, $20
    cp a, $20
    jp z, skip2
    halt
skip2:
    add a, $1
    cp a, $22
    jp z, fail1
    add a, $1
    jp skip3
fail1:
    halt
skip3:

    ; Finish.
    halt
