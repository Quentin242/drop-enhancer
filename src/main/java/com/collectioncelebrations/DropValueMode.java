/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;
public enum DropValueMode
{
	GROUND_ITEMS("Follow Ground Items"),
	GE("Grand Exchange"),
	HIGH_ALCH("High alchemy"),
	HIGHEST("Highest price");
	private final String label;
	DropValueMode(String label)
	{
		this.label = label;
	}
	@Override
	public String toString()
	{
		return label;
	}
}
