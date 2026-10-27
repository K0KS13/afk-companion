package com.jaka.afkcompanion.watch;

/**
 * Decides when the cannonball count is worth a warning: once on the way down past the
 * threshold, and again after any top-up, the same way the core Cannon plugin does it.
 */
public class CannonWarning
{
	private int lastCount;
	private boolean warned;

	/** -1 until the game has told us, so the value sent on login is never news. */
	private int lastDecayed = -1;

	/**
	 * @param count cannonballs loaded in the cannon now
	 * @param threshold warn at or below this many; 0 disables the warning
	 * @return true when a warning should go out
	 */
	public boolean update(int count, int threshold)
	{
		if (count > lastCount)
		{
			warned = false;
		}
		lastCount = count;

		if (threshold <= 0 || count <= 0 || count > threshold || warned)
		{
			return false;
		}

		warned = true;
		return true;
	}

	/**
	 * @param value the cannon's decayed flag, 1 once it has fallen apart
	 * @return true when we saw it go from standing to decayed
	 */
	public boolean decayed(int value)
	{
		final boolean news = lastDecayed == 0 && value == 1;
		lastDecayed = value;
		return news;
	}

	public void reset()
	{
		lastCount = 0;
		warned = false;
		lastDecayed = -1;
	}
}
