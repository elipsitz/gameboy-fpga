; Set the current test ID to the first argument
MACRO SetTestID
    ld hl, $DFFF
    ld [hl], \1
ENDM

; AssertEquals n
; Aborts test suite if A isn't equal to the immediate.
MACRO AssertEquals
    cp a, \1
    jp nz, suite_end
ENDM

SECTION "ROM0", ROM0
    ; ######### Test 1: `ld r, n` and `ld r, r`
    SetTestID 1
    ld a, $13
    AssertEquals $13
    ld b, $30
    ld a, b
    AssertEquals $30

    ; ######## Test 2: Loads to/from (HL)
    SetTestID 2
    ld hl, $C000
    ld [hl], $14
    ld a, [hl]
    AssertEquals $14
    ld a, $60
    ld [hl], a
    ld a, $00
    ld a, [hl]
    AssertEquals $60
    ld b, $70
    ld [hl], b
    ld c, $00
    ld c, [hl]
    ld a, c
    AssertEquals $70

    ; ######### Test 3: Load increment/decrement (with HL)
    SetTestID 3
    ld hl, $C000
    ld a, $50
    ldi [hl], a
    ld a, $54
    ldi [hl], a
    ld a, h
    AssertEquals $C0
    ld a, l
    AssertEquals $02
    ld hl, $C000
    ld a, [hl]
    AssertEquals $50
    ld hl, $C001
    ld a, [hl]
    AssertEquals $54

    ld a, $00
    ldd a, [hl]
    AssertEquals $54
    ldd a, [hl]
    AssertEquals $50
    ld a, h
    AssertEquals $BF
    ld a, l
    AssertEquals $FF

    ld hl, $C000
    ldi a, [hl]
    AssertEquals $50
    ldi a, [hl]
    AssertEquals $54

    ld a, $99
    ldd [hl], a
    ld a, h
    AssertEquals $C0
    ld a, l
    AssertEquals $01
    ld hl, $C002
    ld a, [hl]
    AssertEquals $99

    ; ######### Test 4: 16-bit load immediate
    SetTestID 4
    ld bc, $ABCD
    ld a, b
    AssertEquals $AB
    ld a, c
    AssertEquals $CD
    ld de, $EF12
    ld a, d
    AssertEquals $EF
    ld a, e
    AssertEquals $12

    ; ######### Test 5: Jumps
    SetTestID 5
    ld a, $0
    jp :+
    halt
:   ld a, $1
    AssertEquals $1
    ld hl, :+
    jp hl
    halt
:   ld a, $2
    AssertEquals $2

    ; ######### Test 6: Basic 8-bit ALU
    SetTestID 6
    ld a, 0
    add a, 2
    jp z, suite_end
    jp c, suite_end
    AssertEquals 2
    ld b, 14
    add a, b
    AssertEquals 16
    add a, 240
    jp nz, suite_end
    jp nc, suite_end
    ld a, 200
    add a, 199
    jp nc, suite_end
    AssertEquals $8F
    ld hl, data_2b
    add a, [hl] 
    AssertEquals $BA

    ; ######### Test 7: Loads with register-defined memory addresses
    SetTestID 7
    ld hl, 0
    ld bc, data_40
    ld a, 0
    ld a, [bc]
    AssertEquals $40
    ld de, data_42
    ld a, [de]
    AssertEquals $42
    ld bc, $C000
    ld a, $77
    ld [bc], a
    ld hl, $C000
    AssertEquals [hl]

    ; ######### Test 8: Load to/from A with immediate address
    SetTestID 8
    ld a, [data_2b]
    AssertEquals $2B
    ld a, [$C101]
    ld hl, $C101
    AssertEquals [hl]

    ; ######### Test 9: LDH (Read/write to 0xFFxx)
    SetTestID 9
    ld h, $ff
    ld a, $65
    ldh [$ff00+$80], a
    ld l, $80
    AssertEquals [hl]
    ld l, $81
    ld [hl], $99
    ldh a, [$ff00+$81]
    AssertEquals $99
    ld c, $82
    ld a, $55
    ldh [$ff00+c], a
    ld l, $82
    AssertEquals [hl]
    ld l, $83
    ld [hl], $45
    ld c, l
    ldh a, [$ff00+c]
    AssertEquals $45

    ; ######### Test 10: 16-bit INC/DEC
    SetTestID 10
    ld bc, $0000
    inc bc
    ld a, $00
    AssertEquals b
    ld a, $01
    AssertEquals c
    ;
    dec bc
    ld a, $00
    AssertEquals b
    AssertEquals c
    ;
    ld bc, $FFFF
    inc bc
    ld a, $00
    AssertEquals b
    AssertEquals c
    ;
    dec bc
    ld a, $FF
    AssertEquals b
    AssertEquals c

    ; ######### Test 11: LD (nn), SP
    SetTestID 11
    ld sp, $1234
    ld [$C000], sp
    ld hl, $C000
    ldi a, [hl]
    AssertEquals $34
    ldi a, [hl]
    AssertEquals $12

    ; ######### Test 12: LD SP, HL
    SetTestID 12
    ld sp, $0000
    ld hl, $AABB
    ld sp, hl
    ld [$C000], sp
    ld hl, $C000
    ldi a, [hl]
    AssertEquals $BB
    ldi a, [hl]
    AssertEquals $AA

    ; ######### Test 13: PUSH, POP
    SetTestID 13
    ld bc, $7788
    ld sp, $C002
    push bc
    ld [$C002], sp
    ld hl, $C003
    ldd a, [hl]
    AssertEquals $C0
    ldd a, [hl]
    AssertEquals $00
    ldd a, [hl]
    AssertEquals $77
    ldd a, [hl]
    AssertEquals $88
    ;
    pop de
    ld a, d
    AssertEquals $77
    ld a, e
    AssertEquals $88
    ld [$C010], sp
    ld hl, $C011
    ldd a, [hl]
    AssertEquals $C0
    ldd a, [hl]
    AssertEquals $02

    ; ######### Test 14: ADD HL,rr
    SetTestID 14
    ld hl, $1122
    ld bc, $2233
    add hl, bc
    ld a, h
    AssertEquals $33
    ld a, l
    AssertEquals $55
    ld bc, $22F3
    add hl, bc
    ld a, h
    AssertEquals $56
    ld a, l
    AssertEquals $48
    ; TODO: check flags

    ; ######### Test 15: 16-bit signed add: (add SP,dd) and (ld HL,SP+dd)
    SetTestID 15
    ; SP
    ld sp, $0000
    add sp, -2
    ld [$C000], sp
    ld hl, $C000
    ldi a, [hl]
    AssertEquals $FE
    ldi a, [hl]
    AssertEquals $FF
    ; HL
    ld sp, $00A0
    ld hl, sp + $04
    ld a, h
    AssertEquals $00
    ld a, l
    AssertEquals $A4
    ;
    ld hl, sp + $66
    ld a, h
    AssertEquals $01
    ld a, l
    AssertEquals $06
    ;
    ld sp, $0205
    ld hl, sp - $34
    ld a, h
    AssertEquals $01
    ld a, l
    AssertEquals $D1
    ; TODO: check flags

    ; ######### Test 16: PUSH/POP AF
    SetTestID 16
    ld sp, $C002
    ld a, $40
    sub a, $40
    ; Flags should be 1100
    push af
    ld hl, $C001
    ldd a, [hl]
    AssertEquals $00 ; A
    ld a, [hl]
    AssertEquals $C0 ; F
    ld a, $30
    ldi [hl], a
    ld a, $99
    ldi [hl], a
    ld a, $11
    pop af
    jp z, suite_end ; z should be unset
    AssertEquals $99

    ; ========================================
    ; If we made it here, suite is successful.
    SetTestID 0
suite_end:
    halt

data_2b:
    db $2b
data_40:
    db $40
data_42:
    db $42