/*
    Copyright 2011, Strategic Gains, Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package com.strategicgains.eventing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.strategicgains.eventing.local.LocalEventBus;
import com.strategicgains.eventing.local.LocalEventBusBuilder;


/**
 * @author toddf
 * @since May 18, 2011
 */
public class DomainEventsTest
{
	private DomainEventsTestHandler handler = new DomainEventsTestHandler();
	private DomainEventsTestIgnoredEventsHandler ignoredHandler = new DomainEventsTestIgnoredEventsHandler();
	private DomainEventsTestLongEventHandler longHandler = new DomainEventsTestLongEventHandler();

	@Before
	public void setup()
	{
		EventBus q = new LocalEventBusBuilder()
			.subscribe(handler)
			.subscribe(ignoredHandler)
			.subscribe(longHandler)
			.build();
		DomainEvents.addBus("primary", q);
	}
	
	@After
	public void teardown()
	{
		DomainEvents.shutdown();
	}

	@Test
	public void isSingleton()
	{
		assertTrue(DomainEvents.instance() == DomainEvents.instance());
	}

	@Test
	public void shouldNotifyEventHandler()
	throws Exception
	{
		assertEquals(0, handler.getCallCount());
		DomainEvents.publish(new HandledEvent());
		Thread.sleep(150);
		assertEquals(1, handler.getCallCount());
		assertEquals(0, ignoredHandler.getCallCount());
		assertEquals(0, longHandler.getCallCount());
	}

	@Test
	public void shouldNotifyEventHandlerMultipleTimes()
	throws Exception
	{
		assertEquals(0, handler.getCallCount());
		assertEquals(0, ignoredHandler.getCallCount());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		Thread.sleep(150);
		assertEquals(5, handler.getCallCount());
		assertEquals(5, ignoredHandler.getCallCount());
		assertEquals(0, longHandler.getCallCount());
	}

	@Test
	public void shouldNotNotifyEventHandler()
	throws Exception
	{
		assertEquals(0, ignoredHandler.getCallCount());
		DomainEvents.publish(new IgnoredEvent());
		Thread.sleep(150);
		assertEquals(0, handler.getCallCount());
		assertEquals(1, ignoredHandler.getCallCount());
		assertEquals(0, longHandler.getCallCount());
	}

	@Test
	public void shouldRetryEventHandler()
	throws Exception
	{
		((LocalEventBus) DomainEvents.getBus("primary")).retryOnError(true);
		assertEquals(0, handler.getCallCount());
		DomainEvents.publish(new ErroredEvent());
		Thread.sleep(150);
		assertEquals(6, handler.getCallCount());
		assertEquals(0, ignoredHandler.getCallCount());
		assertEquals(0, longHandler.getCallCount());
	}

	@Test
	public void shouldNotRetryEventHandler()
	throws Exception
	{
		assertEquals(0, handler.getCallCount());
		DomainEvents.publish(new ErroredEvent());
		Thread.sleep(150);
		assertEquals(1, handler.getCallCount());
		assertEquals(0, ignoredHandler.getCallCount());
		assertEquals(0, longHandler.getCallCount());
	}

	@Test
	public void shouldProcessInParallel()
	throws Exception
	{
		assertEquals(0, longHandler.getCallCount());
		DomainEvents.publish(new LongEvent());
		DomainEvents.publish(new LongEvent());
		DomainEvents.publish(new LongEvent());
		DomainEvents.publish(new LongEvent());
		DomainEvents.publish(new LongEvent());
		Thread.sleep(150);
		assertEquals(0, handler.getCallCount());
		assertEquals(0, ignoredHandler.getCallCount());
		System.out.println("longHandler instance=" + longHandler.toString());
		assertEquals(5, longHandler.getCallCount());
	}

	@Test
	public void shouldOnlyPublishSelected()
	throws Exception
	{
		((LocalEventBus) DomainEvents.getBus("primary")).addPublishableEventType(HandledEvent.class);

		assertEquals(0, handler.getCallCount());
		assertEquals(0, ignoredHandler.getCallCount());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		Thread.sleep(150);
		assertEquals(5, handler.getCallCount());
		assertEquals(0, ignoredHandler.getCallCount());
		assertEquals(0, longHandler.getCallCount());
	}

	@Test
	public void shouldPublishMultipleBusses()
	throws Exception
	{
		EventBus q = new LocalEventBusBuilder()
			.subscribe(handler)
			.subscribe(ignoredHandler)
			.subscribe(longHandler)
			.addPublishableEventType(HandledEvent.class)
			.build();
		DomainEvents.addBus("secondary", q);

		assertEquals(0, handler.getCallCount());
		assertEquals(0, ignoredHandler.getCallCount());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		DomainEvents.publish(new HandledEvent());
		DomainEvents.publish(new IgnoredEvent());
		Thread.sleep(150);
		assertEquals(10, handler.getCallCount());
		assertEquals(5, ignoredHandler.getCallCount());
		assertEquals(0, longHandler.getCallCount());
	}

	
	// SECTION: INNER CLASSES

	private class HandledEvent
	{
		public void kerBlooey()
		{
			// do nothing.
		}
	}
	
	private class ErroredEvent
	extends HandledEvent
	{
		private int occurrences = 0;

		@Override
		public void kerBlooey()
		{
			if (occurrences++ < 5)
			{
				throw new RuntimeException("KER-BLOOEY!");
			}
		}
	}
	
	private class IgnoredEvent
	{
	}
	
	private class LongEvent
	{
	}

	private static class DomainEventsTestHandler
	implements EventHandler
	{
		private int callCount = 0;

		@Override
		public void handle(Object event)
		{
			assert(HandledEvent.class.isAssignableFrom(event.getClass()));

			++callCount;
			((HandledEvent) event).kerBlooey();
		}
		
		public int getCallCount()
		{
			return callCount;
		}

		@Override
		public boolean handles(Class<?> eventClass)
		{
			if (HandledEvent.class.isAssignableFrom(eventClass))
			{
				return true;
			}
			
			return false;
		}		
	}

	private static class DomainEventsTestIgnoredEventsHandler
	implements EventHandler
	{
		private int callCount = 0;

		@Override
		public void handle(Object event)
		{
			assert(event.getClass().equals(IgnoredEvent.class));
			++callCount;
		}
		
		public int getCallCount()
		{
			return callCount;
		}

		@Override
		public boolean handles(Class<?> eventClass)
		{
			if (IgnoredEvent.class.isAssignableFrom(eventClass))
			{
				return true;
			}
			
			return false;
		}		
	}

	private static class DomainEventsTestLongEventHandler
	implements EventHandler
	{
		private int callCount = 0;

		@Override
		public void handle(Object event)
		{
			assert(event.getClass().equals(LongEvent.class));
			++callCount;
			try
            {
				// pretend the long event takes 1 second to process...
				System.out.println("Event handler " + this.toString() + " going to sleep..." + callCount);
	            Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
	            e.printStackTrace();
            }
		}
		
		public int getCallCount()
		{
			return callCount;
		}

		@Override
		public boolean handles(Class<?> eventClass)
		{
			if (LongEvent.class.isAssignableFrom(eventClass))
			{
				return true;
			}
			
			return false;
		}		
	}
}
