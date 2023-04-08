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

SECTION "VECTORS", ROM0[$0000]
    ; Reset Vectors
    jp suite_start
    DS 5, $00
    ; 0x08
    DS 8, $00
    ; 0x10
    DS 8, $00
    ; 0x18
    DS 8, $00
    ; 0x20
    DS 8, $00
    ; 0x28
    DS 8, $00
    ; 0x30
    DS 8, $00
    ; 0x38
    DS 8, $00
    ; interrupt vblank
    DS 8, $00
    ; interrupt stat
    DS 8, $00
    ; interrupt timer
    inc a
    ret
    DS 6, $00
    ; interrupt serial
    DS 8, $00
    ; interrupt joypad
    DS 8, $00

SECTION "MAIN", ROM0[$0100]
suite_start:
    ld sp, $C100

    ; ########## Test 1: IE/IF is readable/writable
    SetTestID 1
    di
    ld a, $0A
    ldh [$FFFF], a
    ld a, $00
    ldh a, [$FFFF]
    AssertEquals $0A

    ld a, $05
    ldh [$FF0F], a
    ld a, $00
    ldh a, [$FF0F]
    AssertEquals $05

    ; ########## Test 2: No interrupt called if $IE is 0
    SetTestID 2
    ld a, $00
    ldh [$FFFF], a ; IE = $00
    ld a, $04
    ldh [$FF0F], a ; IF = $04
    ei
    nop
    nop
    nop
    nop
    nop
    AssertEquals $04

    ; ########## Test 3: No interrupt called if IME is 0 (di)
    SetTestID 3
    di
    ld a, $04
    ldh [$FFFF], a ; IE = $04
    ldh [$FF0F], a ; IF = $04
    nop
    nop
    nop
    AssertEquals $04

    ; ########## Test 4: Interrupt called
    SetTestID 4
    ld a, $0
    ldh [$FFFF], a ; IE = $00
    ldh [$FF0F], a ; IF = $00
    ei
    ld a, $04
    ldh [$FFFF], a ; IE = $04
    ldh [$FF0F], a ; IF = $04
    nop
    nop
    nop
    AssertEquals $05
    ldh a, [$FFFF]
    AssertEquals $04
    ldh a, [$FF0F]
    AssertEquals $00

    ; ########## Test 5: Make sure instruction right after is called
    SetTestID 5
    ei
    ld b, $01
    ld a, $04
    ldh [$FFFF], a ; IE = $04
    ldh [$FF0F], a ; IF = $04
    inc b
    AssertEquals $05
    ld a, b
    AssertEquals $02

    ; ########## Test 6: Test that EI has a 1 cycle delay before taking effect
    SetTestID 6
    di
    ld b, $01
    ld a, $04
    ldh [$FFFF], a ; IE = $04
    ldh [$FF0F], a ; IF = $04
    ei
    ld a, $A0
    ; interrupt should fire *now*
    AssertEquals $A1

    ; ########## Test 7: Test HALT (TODO)

    ; ========================================
    ; If we made it here, suite is successful.
    SetTestID 0
suite_end:
    stop
