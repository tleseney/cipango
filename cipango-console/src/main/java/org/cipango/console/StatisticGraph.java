package org.cipango.console;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.cipango.console.util.ObjectNameFactory;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdDefTemplate;
import org.jrobin.core.RrdException;
import org.jrobin.core.Sample;
import org.jrobin.graph.RrdGraph;
import org.jrobin.graph.RrdGraphDef;
import org.jrobin.graph.RrdGraphDefTemplate;
import org.xml.sax.InputSource;

public class StatisticGraph
{
	private static final String RDD_TEMPLATE_FILE_NAME = "rddTemplate.xml";
	
	private static final ObjectName OPERATING_SYSTEM = ObjectNameFactory.create("java.lang:type=OperatingSystem");
	private static final ObjectName GARBAGE_COLLECTORS = ObjectNameFactory.create("java.lang:type=GarbageCollector,*");
	
	public enum GraphType
	{
		CALLS("rddCallsGraphTemplate.xml"),
		MEMORY("rddMemoryGraphTemplate.xml"),
		MESSAGES("rddMessagesGraphTemplate.xml"),
		CPU("rddCpuGraphTemplate.xml");
		
		private RrdGraphDefTemplate _template;
		
		private GraphType(String resourceName)
		{
			try
			{
				InputStream templateGraph = getClass().getResourceAsStream(resourceName);
				_template = new RrdGraphDefTemplate(new InputSource(templateGraph));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		public RrdGraphDefTemplate getTemplate()
		{
			return _template;
		}
	}
		
	private long _refreshPeriod = -1; // To ensure that the stat will start if
										// needed at startup

	private StatisticGraphTask _task;
	private RrdDbPool _rrdPool;
	private String _rrdPath;
	private MBeanServerConnection _connection;
	private String _dataFileName;
	private ObjectName _sessionManger;

	private Timer _statTimer = new Timer("Statistics timer");
	private static Runtime __runtime = Runtime.getRuntime();

	private Logger _logger = Log.getLogger("console");

	private boolean _started = false;
	private boolean _cpuStatAvailable = false;

	public StatisticGraph(MBeanServerConnection connection) throws AttributeNotFoundException,
			InstanceNotFoundException, MBeanException, ReflectionException, IOException, RrdException
	{
		_connection = connection;
		_sessionManger = (ObjectName) _connection.getAttribute(JettyManager.SERVER, "sessionManager");
		_rrdPool = RrdDbPool.getInstance();
	}

	/**
	 * Sets the refresh period for statistics in seconds. If the period has changed, write
	 * statictics immediatly and reschedule the timer with <code>statRefreshPeriod</code>.
	 * 
	 * @param statRefreshPeriod The statistics refresh period in seconds or <code>-1</code> to
	 *            disabled refresh.
	 */
	public void setRefreshPeriod(long statRefreshPeriod)
	{
		if (_refreshPeriod != statRefreshPeriod)
		{
			this._refreshPeriod = statRefreshPeriod;
			if (_task != null)
			{
				_task.run();
				_task.cancel();
			}

			_task = new StatisticGraphTask();

			if (_refreshPeriod != -1)
			{
				_statTimer.schedule(_task, _refreshPeriod * 1000, _refreshPeriod * 1000);
			}
		}
	}
	
	public void reset()
	{
		try
		{
			RrdDb rrdDb = _rrdPool.requestRrdDb(_rrdPath);
			Sample sample = rrdDb.createSample();
			sample.setValue("calls", (Integer) _connection.getAttribute(_sessionManger, "callSessions"));
			long totalMemory = __runtime.totalMemory();
			sample.setValue("maxMemory", __runtime.maxMemory());
			sample.setValue("totalMemory", totalMemory);
			sample.setValue("usedMemory", totalMemory - __runtime.freeMemory());
			// No values are set for incomingMessages, outgoingMessages to reset these counters.
			sample.update();
			_rrdPool.release(rrdDb);
		}
		catch (Exception e)
		{
			_logger.warn("Unable to set statistics", e);
		}
	}

	@SuppressWarnings("unchecked")
	public void updateDb()
	{
		try
		{
			RrdDb rrdDb = _rrdPool.requestRrdDb(_rrdPath);
			Sample sample = rrdDb.createSample();
			if (_connection.isRegistered(_sessionManger))
				sample.setValue("calls", (Integer) _connection.getAttribute(_sessionManger, "callSessions"));
			
			long totalMemory = __runtime.totalMemory();
			sample.setValue("maxMemory", __runtime.maxMemory());
			sample.setValue("totalMemory", totalMemory);
			sample.setValue("usedMemory", totalMemory - __runtime.freeMemory());
			if (_connection.isRegistered(SipManager.CONNECTOR_MANAGER))
			{
				sample.setValue("incomingMessages",
						(Long) _connection.getAttribute(SipManager.CONNECTOR_MANAGER, "messagesReceived"));
				sample.setValue("outgoingMessages",
						(Long) _connection.getAttribute(SipManager.CONNECTOR_MANAGER, "messagesSent"));
			}
			
			int nbCpu = (Integer) _connection.getAttribute(OPERATING_SYSTEM, "AvailableProcessors");
			if (_cpuStatAvailable)
			{
				long processCpuTime = (Long) _connection.getAttribute(OPERATING_SYSTEM, "ProcessCpuTime");
				sample.setValue("cpu", processCpuTime / nbCpu);
			}
			
			long timeInGc = 0;
			Set<ObjectName> garbageCollections = _connection.queryNames(GARBAGE_COLLECTORS, null);
			for (ObjectName objectName : garbageCollections)
			{
				timeInGc += (Long) _connection.getAttribute(objectName, "CollectionTime");
			}
			sample.setValue("timeInGc", timeInGc / nbCpu);
			
			
			sample.update();
			_rrdPool.release(rrdDb);
		}
		catch (Exception e)
		{
			_logger.warn("Unable to set statistics", e);
		}
	}

	public byte[] createGraphAsPng(Date start, Date end, RrdGraphDefTemplate graphTemplate)
	{
		try
		{
			RrdDb rrdDb = _rrdPool.requestRrdDb(_rrdPath);
			_rrdPool.release(rrdDb);
			graphTemplate.setVariable("start", start);
			graphTemplate.setVariable("end", end);
			graphTemplate.setVariable("rrd", _rrdPath);
			// create graph finally
			RrdGraphDef gDef = graphTemplate.getRrdGraphDef();
			RrdGraph graph = new RrdGraph(gDef);
			return graph.getRrdGraphInfo().getBytes();
		}
		catch (Exception e)
		{
			_logger.warn("Unable to create graph", e);
			return null;
		}
	}
	
	/**
	 * Create a graph of the last <code>time</code> seconds.
	 * 
	 * @param time
	 * @return The PNG image.
	 */
	public byte[] createGraphAsPng(long time, String type)
	{
		long start = System.currentTimeMillis() - time * 1000;
		long end = System.currentTimeMillis() - 2500; // Remove last 2,5 seconds due to bug with Jrobin LAST function
		return createGraphAsPng(new Date(start), new Date(end), GraphType.valueOf(type.toUpperCase()).getTemplate());
	}

	public void setDataFileName(String name)
	{
		_dataFileName = name;
	}

	public void start() throws Exception
	{
		if (_started)
			return;
		try
		{
			if (_dataFileName == null)
				_dataFileName = System.getProperty("jetty.home", ".") + "/logs/statistics.rdd";

			File rrdFile = new File(_dataFileName);
			_rrdPath = rrdFile.getAbsolutePath();

			if (_connection.isRegistered(OPERATING_SYSTEM))
			{
				try
				{
					_connection.getAttribute(OPERATING_SYSTEM, "ProcessCpuTime");
					_cpuStatAvailable = true;
				} 
				catch (Throwable e) 
				{
				}
			}
			
			
			if (!rrdFile.exists())
			{
				InputStream templateIs = getClass().getResourceAsStream(RDD_TEMPLATE_FILE_NAME);

				RrdDefTemplate defTemplate = new RrdDefTemplate(new InputSource(templateIs));

				defTemplate.setVariable("path", _rrdPath);
				defTemplate.setVariable("start", new Date(System.currentTimeMillis()));
				RrdDef rrdDef = defTemplate.getRrdDef();

				RrdDb rrdDb = _rrdPool.requestRrdDb(rrdDef);
				rrdDb.getRrdDef().getStep();
				_rrdPool.release(rrdDb);
			}
			else
				updateDb();

			RrdDb rrdDb = _rrdPool.requestRrdDb(_rrdPath);
			setRefreshPeriod(rrdDb.getRrdDef().getStep());
			_rrdPool.release(rrdDb);
			_started = true;
		}
		catch (Exception e)
		{
			_logger.warn("Unable to create RRD", e);
		}
	}

	public boolean isStarted()
	{
		return _started;
	}

	public void stop()
	{
		_started = false;
		if (_task != null)
			_task.cancel();
		_refreshPeriod = -1;
		
	}

	class StatisticGraphTask extends TimerTask
	{

		public void run()
		{
			updateDb();
		}

	}

}
