# Copyright (C) 2022 Xilinx, Inc
# SPDX-License-Identifier: BSD-3-Clause

## Enable bitstream compression
# set_property BITSTREAM.GENERAL.COMPRESS True [current_design]

## Clock signal 125 MHz
# set_property -dict { PACKAGE_PIN H16   IOSTANDARD LVCMOS33 } [get_ports { clk_125mhz }]; #IO_L13P_T2_MRCC_35 Sch=sysclk
# create_clock -add -name clk_125mhz -period 8.00 -waveform {0 4} [get_ports { clk_125mhz }];

## Switches
set_property -dict {PACKAGE_PIN M20 IOSTANDARD LVCMOS33} [get_ports {switches[0]}]
set_property -dict {PACKAGE_PIN M19 IOSTANDARD LVCMOS33} [get_ports {switches[1]}]

## Audio
# set_property -dict {PACKAGE_PIN U9 IOSTANDARD LVCMOS33} [get_ports IIC_1_scl_io]
# set_property PULLUP true [get_ports IIC_1_scl_io];
# set_property -dict {PACKAGE_PIN T9 IOSTANDARD LVCMOS33} [get_ports IIC_1_sda_io]
# set_property PULLUP true [get_ports IIC_1_sda_io];
# set_property -dict { PACKAGE_PIN U5   IOSTANDARD LVCMOS33 } [get_ports audio_clk_10MHz]; 
# set_property -dict { PACKAGE_PIN R18   IOSTANDARD LVCMOS33 } [get_ports bclk]; 
# set_property -dict { PACKAGE_PIN T17   IOSTANDARD LVCMOS33 } [get_ports lrclk]; 
# set_property -dict { PACKAGE_PIN G18   IOSTANDARD LVCMOS33 } [get_ports sdata_o];
# set_property -dict { PACKAGE_PIN F17   IOSTANDARD LVCMOS33 } [get_ports sdata_i]; 
# set_property -dict { PACKAGE_PIN M17   IOSTANDARD LVCMOS33 } [get_ports {codec_addr[0]}]
# set_property -dict { PACKAGE_PIN M18   IOSTANDARD LVCMOS33 } [get_ports {codec_addr[1]}]

## Buttons
set_property -dict {PACKAGE_PIN D19 IOSTANDARD LVCMOS33} [get_ports {buttons[0]}]
set_property -dict {PACKAGE_PIN D20 IOSTANDARD LVCMOS33} [get_ports {buttons[1]}]
set_property -dict {PACKAGE_PIN L20 IOSTANDARD LVCMOS33} [get_ports {buttons[2]}]
set_property -dict {PACKAGE_PIN L19 IOSTANDARD LVCMOS33} [get_ports {buttons[3]}]

## LEDs
set_property -dict {PACKAGE_PIN R14 IOSTANDARD LVCMOS33} [get_ports {leds[0]}]
set_property -dict {PACKAGE_PIN P14 IOSTANDARD LVCMOS33} [get_ports {leds[1]}]
set_property -dict {PACKAGE_PIN N16 IOSTANDARD LVCMOS33} [get_ports {leds[2]}]
set_property -dict {PACKAGE_PIN M14 IOSTANDARD LVCMOS33} [get_ports {leds[3]}]

## RGBLEDs
# set_property -dict { PACKAGE_PIN L15   IOSTANDARD LVCMOS33 } [get_ports { rgbleds_6bits_tri_o[0] }]; 
# set_property -dict { PACKAGE_PIN G17   IOSTANDARD LVCMOS33 } [get_ports { rgbleds_6bits_tri_o[1] }]; 
# set_property -dict { PACKAGE_PIN N15   IOSTANDARD LVCMOS33 } [get_ports { rgbleds_6bits_tri_o[2] }]; 
# set_property -dict { PACKAGE_PIN G14   IOSTANDARD LVCMOS33 } [get_ports { rgbleds_6bits_tri_o[3] }];
# set_property -dict { PACKAGE_PIN L14   IOSTANDARD LVCMOS33 } [get_ports { rgbleds_6bits_tri_o[4] }]; 
# set_property -dict { PACKAGE_PIN M15   IOSTANDARD LVCMOS33 } [get_ports { rgbleds_6bits_tri_o[5] }]; 

## HDMI TX
set_property -dict {PACKAGE_PIN L17 IOSTANDARD TMDS_33} [get_ports hdmi_out_clk_n]
set_property -dict {PACKAGE_PIN L16 IOSTANDARD TMDS_33} [get_ports hdmi_out_clk_p]
set_property -dict {PACKAGE_PIN K18 IOSTANDARD TMDS_33} [get_ports {hdmi_out_data_n[0]}]
set_property -dict {PACKAGE_PIN K17 IOSTANDARD TMDS_33} [get_ports {hdmi_out_data_p[0]}]
set_property -dict {PACKAGE_PIN J19 IOSTANDARD TMDS_33} [get_ports {hdmi_out_data_n[1]}]
set_property -dict {PACKAGE_PIN K19 IOSTANDARD TMDS_33} [get_ports {hdmi_out_data_p[1]}]
set_property -dict {PACKAGE_PIN H18 IOSTANDARD TMDS_33} [get_ports {hdmi_out_data_n[2]}]
set_property -dict {PACKAGE_PIN J18 IOSTANDARD TMDS_33} [get_ports {hdmi_out_data_p[2]}]
set_property -dict {PACKAGE_PIN R19 IOSTANDARD LVCMOS33} [get_ports {hdmi_out_hpd[0]}]

## Pmoda
## RPi GPIO 7-0 are shared with pmoda_rpi_gpio_tri_io[7:0]
# set_property -dict {PACKAGE_PIN Y19 IOSTANDARD LVCMOS33} [get_ports {pmoda_rpi_gpio_tri_io[1]}]
# set_property -dict {PACKAGE_PIN Y18 IOSTANDARD LVCMOS33} [get_ports {pmoda_rpi_gpio_tri_io[0]}]
# set_property -dict {PACKAGE_PIN Y17 IOSTANDARD LVCMOS33} [get_ports {pmoda_rpi_gpio_tri_io[3]}]
# set_property -dict {PACKAGE_PIN Y16 IOSTANDARD LVCMOS33} [get_ports {pmoda_rpi_gpio_tri_io[2]}]
# set_property -dict {PACKAGE_PIN U19 IOSTANDARD LVCMOS33} [get_ports {pmoda_rpi_gpio_tri_io[5]}]
# set_property -dict {PACKAGE_PIN U18 IOSTANDARD LVCMOS33} [get_ports {pmoda_rpi_gpio_tri_io[4]}]
# set_property -dict {PACKAGE_PIN W19 IOSTANDARD LVCMOS33} [get_ports {pmoda_rpi_gpio_tri_io[7]}]
# set_property -dict {PACKAGE_PIN W18 IOSTANDARD LVCMOS33} [get_ports {pmoda_rpi_gpio_tri_io[6]}]
# set_property PULLUP true [get_ports {pmoda_rpi_gpio_tri_io[2]}]
# set_property PULLUP true [get_ports {pmoda_rpi_gpio_tri_io[3]}]
# set_property PULLUP true [get_ports {pmoda_rpi_gpio_tri_io[6]}]
# set_property PULLUP true [get_ports {pmoda_rpi_gpio_tri_io[7]}]

## Pmodb
set_property -dict {PACKAGE_PIN Y14 IOSTANDARD LVCMOS33} [get_ports {ps_i2c_scl}]
set_property -dict {PACKAGE_PIN W14 IOSTANDARD LVCMOS33} [get_ports {ps_i2c_sda}]
# set_property -dict {PACKAGE_PIN T10 IOSTANDARD LVCMOS33} [get_ports {pmodb_gpio_tri_io[3]}]
# set_property -dict {PACKAGE_PIN T11 IOSTANDARD LVCMOS33} [get_ports {pmodb_gpio_tri_io[2]}]
# set_property -dict {PACKAGE_PIN W16 IOSTANDARD LVCMOS33} [get_ports {pmodb_gpio_tri_io[5]}]
# set_property -dict {PACKAGE_PIN V16 IOSTANDARD LVCMOS33} [get_ports {pmodb_gpio_tri_io[4]}]
# set_property -dict {PACKAGE_PIN W13 IOSTANDARD LVCMOS33} [get_ports {pmodb_gpio_tri_io[7]}]
# set_property -dict {PACKAGE_PIN V12 IOSTANDARD LVCMOS33} [get_ports {pmodb_gpio_tri_io[6]}]
set_property PULLUP true [get_ports {ps_i2c_scl}]
set_property PULLUP true [get_ports {ps_i2c_sda}]
# set_property PULLUP true [get_ports {pmodb_gpio_tri_io[6]}]
# set_property PULLUP true [get_ports {pmodb_gpio_tri_io[7]}]

## Arduino GPIO
set_property -dict {PACKAGE_PIN T14 IOSTANDARD LVCMOS33} [get_ports { cartridge_dir_ctrl }]
set_property -dict {PACKAGE_PIN U12 IOSTANDARD LVCMOS33} [get_ports { cartridge_nWR      }]
set_property -dict {PACKAGE_PIN U13 IOSTANDARD LVCMOS33} [get_ports { cartridge_nCS      }]
set_property -dict {PACKAGE_PIN V13 IOSTANDARD LVCMOS33} [get_ports { cartridge_A[1]     }]
set_property -dict {PACKAGE_PIN V15 IOSTANDARD LVCMOS33} [get_ports { cartridge_A[3]     }]
set_property -dict {PACKAGE_PIN T15 IOSTANDARD LVCMOS33} [get_ports { cartridge_A[5]     }]
set_property -dict {PACKAGE_PIN R16 IOSTANDARD LVCMOS33} [get_ports { cartridge_A[7]     }]
set_property -dict {PACKAGE_PIN U17 IOSTANDARD LVCMOS33} [get_ports { cartridge_dir_A_hi }]

set_property -dict {PACKAGE_PIN V17 IOSTANDARD LVCMOS33} [get_ports { cartridge_A[12] }]
set_property -dict {PACKAGE_PIN V18 IOSTANDARD LVCMOS33} [get_ports { cartridge_A[14] }]
set_property -dict {PACKAGE_PIN T16 IOSTANDARD LVCMOS33} [get_ports { cartridge_D[0] }]
set_property -dict {PACKAGE_PIN R17 IOSTANDARD LVCMOS33} [get_ports { cartridge_D[2] }]
set_property -dict {PACKAGE_PIN P18 IOSTANDARD LVCMOS33} [get_ports { cartridge_D[4] }]
set_property -dict {PACKAGE_PIN N17 IOSTANDARD LVCMOS33} [get_ports { cartridge_D[6] }]
set_property -dict {PACKAGE_PIN Y13 IOSTANDARD LVCMOS33} [get_ports { cartridge_dir_D }]
set_property -dict {PACKAGE_PIN P16 IOSTANDARD LVCMOS33} [get_ports { cartridge_dir_nRST }]
set_property -dict {PACKAGE_PIN P15 IOSTANDARD LVCMOS33} [get_ports { cartridge_dir_VIN }]

## Raspberry PI 
set_property -dict { PACKAGE_PIN W18  IOSTANDARD LVCMOS33 } [get_ports { link_out            }];
set_property -dict { PACKAGE_PIN W19  IOSTANDARD LVCMOS33 } [get_ports { link_dir_out        }];
set_property -dict { PACKAGE_PIN Y18  IOSTANDARD LVCMOS33 } [get_ports { link_in             }];
set_property -dict { PACKAGE_PIN V6   IOSTANDARD LVCMOS33 } [get_ports { link_dir_in         }];
set_property -dict { PACKAGE_PIN Y6   IOSTANDARD LVCMOS33 } [get_ports { link_data           }];
set_property -dict { PACKAGE_PIN U7   IOSTANDARD LVCMOS33 } [get_ports { link_dir_data       }];
set_property -dict { PACKAGE_PIN C20  IOSTANDARD LVCMOS33 } [get_ports { link_clock          }];
set_property -dict { PACKAGE_PIN V7   IOSTANDARD LVCMOS33 } [get_ports { link_dir_clock      }];
set_property -dict { PACKAGE_PIN U8   IOSTANDARD LVCMOS33 } [get_ports { cartridge_PHI       }];
set_property -dict { PACKAGE_PIN W6   IOSTANDARD LVCMOS33 } [get_ports { cartridge_nRD       }];
set_property -dict { PACKAGE_PIN Y7   IOSTANDARD LVCMOS33 } [get_ports { cartridge_A[0]      }];
set_property -dict { PACKAGE_PIN V8   IOSTANDARD LVCMOS33 } [get_ports { cartridge_A[2]      }];
set_property -dict { PACKAGE_PIN V10  IOSTANDARD LVCMOS33 } [get_ports { cartridge_A[4]      }];
set_property -dict { PACKAGE_PIN F20  IOSTANDARD LVCMOS33 } [get_ports { cartridge_A[6]      }];
set_property -dict { PACKAGE_PIN W10  IOSTANDARD LVCMOS33 } [get_ports { cartridge_dir_A_lo  }];
set_property -dict { PACKAGE_PIN F19  IOSTANDARD LVCMOS33 } [get_ports { cartridge_A[8]      }];
set_property -dict { PACKAGE_PIN U19  IOSTANDARD LVCMOS33 } [get_ports { cartridge_A[9]      }];
set_property -dict { PACKAGE_PIN Y16  IOSTANDARD LVCMOS33 } [get_ports { cartridge_A[10]     }];
set_property -dict { PACKAGE_PIN Y17  IOSTANDARD LVCMOS33 } [get_ports { cartridge_A[11]     }];
set_property -dict { PACKAGE_PIN Y19  IOSTANDARD LVCMOS33 } [get_ports { cartridge_A[13]     }];
set_property -dict { PACKAGE_PIN U18  IOSTANDARD LVCMOS33 } [get_ports { cartridge_A[15]     }];
set_property -dict { PACKAGE_PIN B20  IOSTANDARD LVCMOS33 } [get_ports { cartridge_D[1]      }];
set_property -dict { PACKAGE_PIN W8   IOSTANDARD LVCMOS33 } [get_ports { cartridge_D[3]      }];
set_property -dict { PACKAGE_PIN Y8   IOSTANDARD LVCMOS33 } [get_ports { cartridge_D[5]      }];
set_property -dict { PACKAGE_PIN B19  IOSTANDARD LVCMOS33 } [get_ports { cartridge_D[7]      }];
set_property -dict { PACKAGE_PIN W9   IOSTANDARD LVCMOS33 } [get_ports { cartridge_n_oe      }];
set_property -dict { PACKAGE_PIN A20  IOSTANDARD LVCMOS33 } [get_ports { cartridge_nRST      }];
set_property -dict { PACKAGE_PIN Y9   IOSTANDARD LVCMOS33 } [get_ports { cartridge_VIN       }];
