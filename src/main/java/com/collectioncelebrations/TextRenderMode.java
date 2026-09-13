/* Copyright (c) 2026 maiz. BSD-2-Clause; see LICENSE. */
package com.collectioncelebrations;
public enum TextRenderMode
{
	CRISP("Crisp"),
	SMOOTH("Smooth"),
	SMOOTH_OUTLINED("Smooth outlined");
	private final String label;
	TextRenderMode(String label)
	{
		this.label = label;
	}
	@Override
	public String toString()
	{
		return label;
	}
}
