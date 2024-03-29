SECTION "MAIN", ROM0[$0100]
    ld b, $AB
    ld c, b
    ld d, $06
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

    ; Test ALU
    ld hl, $C004
    ld a, $00
    ;
    add a, $10
    ldi [hl], a
    ;
    sub a, $03
    ldi [hl], a
    ;
    cp a, $0D
    ldi [hl], a
    ;
    ld [hl], $40
    add a, [hl]
    ldi [hl], a
    ;
    ld b, $1D
    sub a, b
    ldi [hl], a
    ;
    ld a, $A0
    jp skip1
    ld a, $B0
    halt
skip1:
    add a, $01
    ld hl, skip2
    jp hl
    ld a, $B1
    halt
skip2:
    add a, $01
    cp a, $A2 ; set z
    jp z, skip3
    ld a, $B2
    halt
skip3:
    add a, $01
    cp a, $99 ; UNset z
    jp z, fail1
    jp skip4
fail1:
    ld a, $B3
    halt
skip4:
    nop
    
    ; End the test.
    stop

data: db $AA