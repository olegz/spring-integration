package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.integration.test.matcher.PayloadAndHeaderMatcher.sameExceptIgnorableHeaders;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageGroup;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcMessageStoreTests {

	@Autowired
	private DataSource dataSource;

	private JdbcMessageStore messageStore;

	@Before
	public void init() {
		messageStore = new JdbcMessageStore(dataSource);
	}

	@Test
	@Transactional
	public void testGetNonExistent() throws Exception {
		Message<?> result = messageStore.getMessage(UUID.randomUUID());
		assertNull(result);
	}

	@Test
	@Transactional
	public void testAddAndGet() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		assertNull(messageStore.getMessage(message.getHeaders().getId()));
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertNotNull(result);
		assertThat(saved, sameExceptIgnorableHeaders(result));
		assertNotNull(result.getHeaders().get(JdbcMessageStore.SAVED_KEY));
		assertNotNull(result.getHeaders().get(JdbcMessageStore.CREATED_DATE_KEY));
	}

	@Test
	@Transactional
	public void testAddAndGetWithDifferentRegion() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		messageStore.setRegion("FOO");
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertNull(result);
	}

	@Test
	@Transactional
	public void testAddAndUpdate() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId("X").build();
		message = messageStore.addMessage(message);
		message = MessageBuilder.fromMessage(message).setCorrelationId("Y").build();
		message = messageStore.addMessage(message);
		assertEquals("Y", messageStore.getMessage(message.getHeaders().getId()).getHeaders().getCorrelationId());
	}

	@Test
	@Transactional
	public void testAddAndUpdateAlreadySaved() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		Message<String> result = messageStore.addMessage(message);
		assertEquals(message, result);
	}

	@Test
	@Transactional
	public void testAddAndUpdateAlreadySavedAndCopied() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		Message<String> copy = MessageBuilder.fromMessage(saved).build();
		Message<String> result = messageStore.addMessage(copy);
		assertNotSame(copy, result);
		assertThat(saved, sameExceptIgnorableHeaders(result, JdbcMessageStore.CREATED_DATE_KEY));
		assertNotNull(messageStore.getMessage(saved.getHeaders().getId()));
	}

	@Test
	@Transactional
	public void testAddAndDelete() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		assertNotNull(messageStore.removeMessage(message.getHeaders().getId()));
	}

	@Test
	@Transactional
	public void testAddAndGetMessageGroup() throws Exception {
		String correlationId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(correlationId).build();
		long now = System.currentTimeMillis();
		messageStore.addMessageToGroup(correlationId, message);
		MessageGroup group = messageStore.getMessageGroup(correlationId);
		assertEquals(1, group.size());
		assertTrue("Timestamp too early: " + group.getTimestamp() + "<" + now, group.getTimestamp() >= now);
	}

	@Test
	@Transactional
	public void testAddAndMarkMessageGroup() throws Exception {
		String correlationId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(correlationId).build();
		messageStore.addMessageToGroup(correlationId, message);
		MessageGroup group = messageStore.getMessageGroup(correlationId);
		messageStore.markMessageGroup(group);
		assertEquals(1, group.getMarked().size());
	}

	@Test
	@Transactional
	public void testExpireMessageGroup() throws Exception {
		String correlationId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(correlationId).build();
		messageStore.addMessageToGroup(correlationId, message);
		messageStore.expireMessageGroups(-10000);
		MessageGroup group = messageStore.getMessageGroup(correlationId);
		assertEquals(0, group.getMarked().size());
	}

}
