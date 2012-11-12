package org.cipango.util;

public class TimerTask implements Comparable<TimerTask>
{
	private Runnable _runnable;
	private long _executionTime;
	private boolean _cancelled = false;
	
	public TimerTask(Runnable runnable, long executionTime)
	{
		_runnable = runnable;
		_executionTime = executionTime;
	}
	
	public long getExecutionTime()
	{
		return _executionTime;
	}
	
	public Runnable getRunnable()
	{
		return _runnable;
	}
	
	public int compareTo(TimerTask task)
	{
		long otherExecutionTime = task._executionTime;
		return _executionTime < otherExecutionTime ? -1 : (_executionTime == otherExecutionTime ? 0 : 1);
	}
	
	public boolean isCancelled()
	{
		return _cancelled;
	}
	
	public void cancel()
	{
		_cancelled = true;
	}
	
	public String toString()
	{
		long delay = _executionTime - System.currentTimeMillis();
		if (delay > 1000 || delay < -1000)
			return _runnable + "@" + (_executionTime - System.currentTimeMillis()) / 1000 + "s";
		else
			return _runnable + "@" + (_executionTime - System.currentTimeMillis()) + "ms";
	}
}