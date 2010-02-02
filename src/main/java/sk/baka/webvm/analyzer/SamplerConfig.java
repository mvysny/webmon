/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebMon is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebMon.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer;

/**
 * Configures the sampler.
 * @author Martin Vysny
 */
public final class SamplerConfig {

	private final int historyLength;
	private final int initialDelay;

	/**
	 * Sleep x milliseconds before sampling start.
	 * @return
	 */
	public int getInitialDelay() {
		return initialDelay;
	}

	/**
	 * A number of samples to keep in the history.
	 * @return
	 */
	public int getHistoryLength() {
		return historyLength;
	}

	/**
	 * Sample each x milliseconds
	 * @return
	 */
	public int getHistorySampleDelayMs() {
		return historySampleDelayMs;
	}
	private final int historySampleDelayMs;

	/**
	 * Creates new object.
	 * @param historyLength a number of samples to keep in the history.
	 * @param historySampleDelayMs sample each x milliseconds
	 * @param initialDelay sleep x milliseconds before sampling start
	 */
	public SamplerConfig(final int historyLength, final int historySampleDelayMs, final int initialDelay) {
		this.historyLength = historyLength;
		this.historySampleDelayMs = historySampleDelayMs;
		this.initialDelay = initialDelay;
	}
}
