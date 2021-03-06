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

#include "b_BasicEm/Functions.h"
#include "b_APIEm/FaceFinder.h"
#include "b_APIEm/BFFaceFinder.h"

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

void bpi_FaceFinder_init( struct bbs_Context* cpA,
					      struct bpi_FaceFinder* ptrA )
{
	ptrA->typeE = 0;
	ptrA->vpSetParamsE = NULL;
	ptrA->vpSetRangeE = NULL;
	ptrA->vpProcessE = NULL;
	ptrA->vpPutDcrE = NULL;
	ptrA->vpGetDcrE = NULL;
}

/* ------------------------------------------------------------------------- */

void bpi_FaceFinder_exit( struct bbs_Context* cpA,
						  struct bpi_FaceFinder* ptrA )
{
	ptrA->typeE = 0;
	ptrA->vpSetParamsE = NULL;
	ptrA->vpSetRangeE = NULL;
	ptrA->vpProcessE = NULL;
	ptrA->vpPutDcrE = NULL;
	ptrA->vpGetDcrE = NULL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bpi_FaceFinder_copy( struct bbs_Context* cpA,
						  struct bpi_FaceFinder* ptrA, 
						  const struct bpi_FaceFinder* srcPtrA )
{
	ptrA->typeE = srcPtrA->typeE;
	ptrA->vpSetParamsE = srcPtrA->vpSetParamsE;
	ptrA->vpSetRangeE = srcPtrA->vpSetRangeE;
	ptrA->vpProcessE = srcPtrA->vpProcessE;
	ptrA->vpPutDcrE = srcPtrA->vpPutDcrE;
	ptrA->vpGetDcrE = srcPtrA->vpGetDcrE;
}

/* ------------------------------------------------------------------------- */

flag bpi_FaceFinder_equal( struct bbs_Context* cpA,
						   const struct bpi_FaceFinder* ptrA, 
						   const struct bpi_FaceFinder* srcPtrA )
{

	if( ptrA->typeE != srcPtrA->typeE ) return FALSE;
	if( ptrA->vpSetParamsE != srcPtrA->vpSetParamsE ) return FALSE;
	if( ptrA->vpSetRangeE != srcPtrA->vpSetRangeE ) return FALSE;
	if( ptrA->vpProcessE != srcPtrA->vpProcessE ) return FALSE;
	if( ptrA->vpPutDcrE != srcPtrA->vpPutDcrE ) return FALSE;
	if( ptrA->vpGetDcrE != srcPtrA->vpGetDcrE ) return FALSE;
	return TRUE;
}

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
	
uint32 bpi_FaceFinder_memSize( struct bbs_Context* cpA,
							   const struct bpi_FaceFinder* ptrA )
{
	uint32 memSizeL = 0;
	memSizeL += bbs_SIZEOF16( ptrA->typeE );
	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bpi_FaceFinder_memWrite( struct bbs_Context* cpA,
							    const struct bpi_FaceFinder* ptrA, 
							    uint16* memPtrA )
{
	uint32 memSizeL = bpi_FaceFinder_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &ptrA->typeE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bpi_FaceFinder_memRead( struct bbs_Context* cpA,
							   struct bpi_FaceFinder* ptrA, 
							   const uint16* memPtrA )
{
	bbs_DEF_fNameL( "uint32 bpi_FaceFinder_memRead( struct bbs_Context* cpA, struct bpi_FaceFinder* ptrA, const uint16* memPtrA )" )
	uint32 typeL;

	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &typeL, memPtrA );

	if( typeL != ptrA->typeE )
	{
		bbs_ERROR1( "%s:\nObject type mismatch! Attempt to read an incorrect object.", fNameL );
		return 0;
	}

	return bpi_FaceFinder_memSize( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

void bpi_faceFinderInit( struct bbs_Context* cpA,
					  	 struct bpi_FaceFinder* ptrA,
						 enum bpi_FaceFinderType typeA )
{
	switch( typeA )
	{
		case bpi_FF_BF_FACE_FINDER:		bpi_BFFaceFinder_init( cpA,		( struct bpi_BFFaceFinder* )ptrA ); return; 
			
		default: bbs_ERROR0( "bpi_faceFinderInit: invalid type" );
	}
}

/* ------------------------------------------------------------------------- */

void bpi_faceFinderExit( struct bbs_Context* cpA, 
					     struct bpi_FaceFinder* ptrA )
{
	switch( ptrA->typeE )
	{
		case bpi_FF_BF_FACE_FINDER:	bpi_BFFaceFinder_exit( cpA,		( struct bpi_BFFaceFinder* )ptrA ); return;

		default: bbs_ERROR0( "bpi_faceFinderExit: invalid type" );
	}
}

/* ------------------------------------------------------------------------- */

uint32 bpi_faceFinderMemSize( struct bbs_Context* cpA, 
							  const struct bpi_FaceFinder* ptrA )
{
	switch( ptrA->typeE )
	{
		case bpi_FF_BF_FACE_FINDER:	return bpi_BFFaceFinder_memSize( cpA,	( struct bpi_BFFaceFinder* )ptrA );

		default: bbs_ERROR0( "bpi_faceFinderExit: invalid type" );
	}
	return 0;
}

/* ------------------------------------------------------------------------- */

uint32 bpi_faceFinderMemWrite( struct bbs_Context* cpA, 
							   const struct bpi_FaceFinder* ptrA, uint16* memPtrA )
{
	switch( ptrA->typeE )
	{
		case bpi_FF_BF_FACE_FINDER:		return bpi_BFFaceFinder_memWrite( cpA,	( struct bpi_BFFaceFinder* )ptrA, memPtrA  );

		default: bbs_ERROR0( "bpi_faceFinderMemWrite: invalid type" );
	}
	return 0;
}

/* ------------------------------------------------------------------------- */

uint32 bpi_faceFinderMemRead( struct bbs_Context* cpA,
							  struct bpi_FaceFinder* ptrA, 
							  const uint16* memPtrA,
							  struct bbs_MemTbl* mtpA )
{
	switch( ptrA->typeE )
	{
		case bpi_FF_BF_FACE_FINDER:	return bpi_BFFaceFinder_memRead( cpA,	( struct bpi_BFFaceFinder* )ptrA, memPtrA, mtpA );

		default: bbs_ERROR0( "bpi_faceFinderMemRead: invalid type" );
	}
	return 0;
}

/* ------------------------------------------------------------------------- */

uint32 bpi_faceFinderSizeOf16( struct bbs_Context* cpA, enum bpi_FaceFinderType typeA )
{
	switch( typeA )
	{
		case bpi_FF_BF_FACE_FINDER:	return bbs_SIZEOF16( struct bpi_BFFaceFinder );

		default: bbs_ERROR0( "bpi_faceFinderSizeOf16: invalid type" );
	}
	return 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

