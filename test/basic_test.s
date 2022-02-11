SECTION "ROM0", ROM0
    ld b, $AB
    ld c, b
    ld d, [hl]
    ld h, $C0
    ld l, $00
    ld [hl], d
    ld e, [hl]
    ld [hl], $99
    ld a, [hl]
    
    halt