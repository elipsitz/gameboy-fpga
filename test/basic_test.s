SECTION "ROM0", ROM0
    ld b, $AB
    ld c, b
    ld d, [hl]
    ld hl, $C000
    ld [hl], d
    ld e, [hl]
    ld [hl], $99
    ld a, [hl]

    ; Test the load/increment HL instructions.
    ld a, $50
    ldi [hl], a
    ld a, $51
    ldi [hl], a

    ld hl, $C002
    ld [hl], $52
    ld hl, $C003
    ld [hl], $53
    ldd a, [hl]
    ldd a, [hl]
    ld hl, $C004
    ld [hl], a

    ld hl, $C000
    ldi a, [hl]
    
    ; End the test.
    halt

data: db $AA