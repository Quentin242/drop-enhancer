/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;
public enum PreviewKind
{
	NEW_UNLOCK("New unlock · gold effect"),
	REPEAT_DROP("Repeat drop · rarity border");
	private final String label;
	PreviewKind(String label)
	{
		this.label = label;
	}
	@Override
	public String toString()
	{
		return label;
	}
}
