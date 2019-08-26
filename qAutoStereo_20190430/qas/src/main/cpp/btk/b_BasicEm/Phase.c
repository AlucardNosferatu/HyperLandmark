/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Phase.h"
#include "b_BasicEm/Math.h"

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ auxiliary functions } ---------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ constructor / destructor } ----------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/* ------------------------------------------------------------------------- */

#ifndef bbs_SIN_INTERPOLATION_METHOD_2
const int32 bbs_sin32_table1G[] = 
{
	0,			1608,	411648,		1607,	823040,		1606,	1234176,	1602, 
	1644288,	1599,	2053632,	1594,	2461696,	1588,	2868224,	1581, 
	3272960,	1574,	3675904,	1564,	4076288,	1556,	4474624,	1545, 
	4870144,	1533,	5262592,	1521,	5651968,	1508,	6038016,	1493, 
	6420224,	1478,	6798592,	1463,	7173120,	1445,	7543040,	1428, 
	7908608,	1409,	8269312,	1390,	8625152,	1369,	8975616,	1348, 
	9320704,	1327,	9660416,	1303,	9993984,	1280,	10321664,	1256, 
	10643200,	1231,	10958336,	1205,	11266816,	1178,	11568384,	1151, 
	11863040,	1124,	12150784,	1094,	12430848,	1066,	12703744,	1036, 
	12968960,	1005,	13226240,	974,	13475584,	942,	13716736,	910, 
	13949696,	877,	14174208,	844,	14390272,	810,	14597632,	775, 
	14796032,	741,	14985728,	705,	15166208,	670,	15337728,	634, 
	15500032,	597,	15652864,	561,	15796480,	523,	15930368,	486, 
	16054784,	448,	16169472,	409,	16274176,	372,	16369408,	333, 
	16454656,	295,	16530176,	255,	16595456,	217,	16651008,	177, 
	16696320,	138,	16731648,	99,		16756992,	59,		16772096,	20, 
	16777216,	-20,	16772096,	-59,	16756992,	-99,	16731648,	-138, 
	16696320,	-177,	16651008,	-217,	16595456,	-255,	16530176,	-295, 
	16454656,	-333,	16369408,	-372,	16274176,	-409,	16169472,	-448, 
	16054784,	-486,	15930368,	-523,	15796480,	-561,	15652864,	-597, 
	15500032,	-634,	15337728,	-670,	15166208,	-705,	14985728,	-741, 
	14796032,	-775,	14597632,	-810,	14390272,	-844,	14174208,	-877, 
	13949696,	-910,	13716736,	-942,	13475584,	-974,	13226240,	-1005, 
	12968960,	-1036,	12703744,	-1066,	12430848,	-1094,	12150784,	-1124, 
	11863040,	-1151,	11568384,	-1178,	11266816,	-1205,	10958336,	-1231,
	10643200,	-1256,	10321664,	-1280,	9993984,	-1303,	9660416,	-1327, 
	9320704,	-1348,	8975616,	-1369,	8625152,	-1390,	8269312,	-1409, 
	7908608,	-1428,	7543040,	-1445,	7173120,	-1463,	6798592,	-1478, 
	6420224,	-1493,	6038016,	-1508,	5651968,	-1521,	5262592,	-1533, 
	4870144,	-1545,	4474624,	-1556,	4076288,	-1564,	3675904,	-1574, 
	3272960,	-1581,	2868224,	-1588,	2461696,	-1594,	2053632,	-1599, 
	1644288,	-1602,	1234176,	-1606,	823040,		-1607,	411648,		-1608
};
#else
const int32 bbs_sin32_table2G[] = 
{
	0,			12907,	-122, 
	209469440,	12662,	-368, 
	410894336,	11926,	-596, 
	596525056,	10733,	-802, 
	759234560,	9129,	-978, 
	892780544,	7168,	-1112, 
	992002048,	4939,	-1210, 
	1053097984, 2516,	-1256, 
	1073741824, -4,		-1256, 
	1053097984, -2519,	-1210, 
	992002048,	-4944,	-1112, 
	892780544,	-7173,	-978, 
	759234560,	-9129,	-802, 
	596525056,	-10734, -596, 
	410894336,	-11926, -368, 
	209469440,	-12663,	-122
};
#endif

int32 bbs_sin32( phase16 phaseA )
{
#ifndef bbs_SIN_INTERPOLATION_METHOD_2

	int32 oL = ( phaseA & 0x00FF );
	uint16  indexL = ( ( phaseA & 0x7F00 ) >> 8 ) << 1;
	int32 sinL = bbs_sin32_table1G[ indexL ] + oL * bbs_sin32_table1G[ indexL + 1 ];

	if( ( phaseA & 0x8000 ) != 0 )
	{
		return -sinL;
	}
	else
	{
		return sinL;
	}

#else /*bbs_SIN_INTERPOLATION_METHOD_2*/

	int32 o1L = ( phaseA & 0x07FF );
	int32 o2L = ( o1L * o1L ) >> 8;
	uint16 indexL = ( ( phaseA & 0x7800 ) >> 11 ) * 3;
	int32 sinL = bbs_sin32_table2G[ indexL ] + ( ( o1L * bbs_sin32_table2G[ indexL + 1 ] )  << 3 ) + o2L * bbs_sin32_table2G[ indexL + 2 ];

	if( ( phaseA & 0x8000 ) != 0 )
	{
		return -sinL >> 6;
	}
	else
	{
		return sinL >> 6;
	}

#endif /*bbs_SIN_INTERPOLATION_METHOD_2*/
}

/** computation of sine tables (do not uncomment or remove)
void sin1Table()
{
	long iL;
	for( iL = 0; iL < 128; iL++ )
	{
		int32 phase1L = iL * 256;
		int32 phase2L = phase1L + 256;
		double angle1L = ( M_PI * phase1L ) / 32768;
		double angle2L = ( M_PI * phase2L ) / 32768;
		int32 sin1L = ( sin( angle1L ) * 65536 );
		int32 sin2L = ( sin( angle2L ) * 65536 );
		int32 diffL = sin2L - sin1L;
		eout << iL << ": " << ( sin1L << 8 ) << " + oL * " << diffL << endl;
	}
}

void sin2Table()
{
	long iL;
	for( iL = 0; iL < 16; iL++ )
	{
		int32 p0L = iL  * ( 1 << 11 );
		int32 p1L = p0L + ( 1 << 10 );
		int32 p2L = p0L + ( 1 << 11 );

		double a0L = ( M_PI * p0L ) / ( 1 << 15 );
		double a1L = ( M_PI * p1L ) / ( 1 << 15 );
		double a2L = ( M_PI * p2L ) / ( 1 << 15 );

		int32 s0L = ( sin( a0L ) * ( 1 << 16 ) );
		int32 s1L = ( sin( a1L ) * ( 1 << 16 ) );
		int32 s2L = ( sin( a2L ) * ( 1 << 16 ) );

		int32 aL = 4 * s1L - 3 * s0L - s2L;
		int32 bL = 2 * s2L + 2 * s0L - 4 * s1L;

		eout << iL << ": " << ( s0L << 14 ) << " + ( ( o1L * " << aL << " ) << 3 )"
			 << " + o2L * " << bL << endl;
	}
}
*/

/* ------------------------------------------------------------------------- */

int32 bbs_cos32( phase16 phaseA )
{
	return bbs_sin32( ( phase16 )( phaseA + bbs_M_PI_2_16 ) );
}

/* ------------------------------------------------------------------------- */

int16 bbs_sin16( phase16 phaseA )
{
	return bbs_sin32( phaseA ) >> 10;
}

/* ------------------------------------------------------------------------- */

int16 bbs_cos16( phase16 phaseA )
{
	return bbs_sin32( ( phase16 )( phaseA + bbs_M_PI_2_16 ) ) >> 10;
}

/* ------------------------------------------------------------------------- */

const int32 bbs_atan16_tableG[] =
{
	0,			325,	332800,		326,	666624,		326,	1000448,	325, 
	1333248,	324,	1665024,	323,	1995776,	323,	2326528,	322, 
	2656256,	320,	2983936,	319,	3310592,	317,	3635200,	316, 
	3958784,	314,	4280320,	312,	4599808,	310,	4917248,	308, 
	5232640,	306,	5545984,	303,	5856256,	301,	6164480,	298, 
	6469632,	296,	6772736,	292,	7071744,	291,	7369728,	287, 
	7663616,	284,	7954432,	281,	8242176,	279,	8527872,	275, 
	8809472,	272,	9088000,	269,	9363456,	265,	9634816,	263, 
	9904128,	259,	10169344,	256,	10431488,	252,	10689536,	249, 
	10944512,	246,	11196416,	243,	11445248,	239,	11689984,	236, 
	11931648,	233,	12170240,	230,	12405760,	226,	12637184,	223, 
	12865536,	219,	13089792,	217,	13312000,	213,	13530112,	210, 
	13745152,	207,	13957120,	204,	14166016,	201,	14371840,	198, 
	14574592,	195,	14774272,	192,	14970880,	189,	15164416,	186, 
	15354880,	183,	15542272,	180,	15726592,	178,	15908864,	175, 
	16088064,	172,	16264192,	169,	16437248,	167,	16608256,	165
};

phase16 bbs_atan16( uint32 valA )
{
	uint32 oL = valA & 0x03FF;
	uint16 indexL = ( valA >> 10 ) << 1;
	uint32 phaseL = bbs_atan16_tableG[ indexL ] + oL * bbs_atan16_tableG[ indexL + 1 ];
	return ( phase16 )( phaseL >> 11 );
}

/* ------------------------------------------------------------------------- */

phase16 bbs_phase16( int32 xA, int32 yA )
{
	uint32 xL = ( xA > 0 ) ? xA : -xA;
	uint32 yL = ( yA > 0 ) ? yA : -yA;
	phase16 phaseL;

	if( xL == 0 && yL == 0 ) return 0;

	if( xL == yL )
	{
		phaseL = bbs_M_PI_4_16; /*PI/4*/
	}
	else if( xL > yL )
	{
		if( yL >= 65536 ) /* avoid overflow (1 << 16) */
		{
			uint32 shiftL = bbs_intLog2( yL ) - 15;
			xL >>= shiftL;
			yL >>= shiftL;
		}
		phaseL = bbs_atan16( ( yL << 16 ) / xL );
	}
	else
	{
		if( xL >= 65536 ) /* avoid overflow (1 << 16) */
		{
			uint32 shiftL = bbs_intLog2( xL ) - 15;
			xL >>= shiftL;
			yL >>= shiftL;
		}
		phaseL = bbs_M_PI_2_16 - bbs_atan16( ( xL << 16 ) / yL );
	}

	if( xA >= 0 )
	{
		if( yA >= 0 )
		{
			return phaseL;
		}
		else
		{
			return -phaseL;
		}
	}
	else
	{
		if( yA >= 0 )
		{
			return bbs_M_PI_16 - phaseL;
		}
		else
		{
			return phaseL - bbs_M_PI_16;
		}
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


