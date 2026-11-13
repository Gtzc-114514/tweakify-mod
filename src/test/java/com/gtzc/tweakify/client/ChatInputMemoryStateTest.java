package com.gtzc.tweakify.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatInputMemoryStateTest {
	@BeforeEach
	void resetState() {
		ChatInputMemoryState.resetForTests();
	}

	@Test
	void startsEmpty() {
		assertEquals("", ChatInputMemoryState.savedText());
	}

	@Test
	void closedDraftIsRestored() {
		ChatInputMemoryState.remember("hello world");
		assertEquals("hello world", ChatInputMemoryState.savedText());
	}

	@Test
	void nullIsTreatedAsEmpty() {
		ChatInputMemoryState.remember("text");
		ChatInputMemoryState.remember(null);
		assertEquals("", ChatInputMemoryState.savedText());
	}

	@Test
	void blankIsTreatedAsEmpty() {
		ChatInputMemoryState.remember("   ");
		assertEquals("", ChatInputMemoryState.savedText());
	}

	@Test
	void surroundingSpacesArePreserved() {
		ChatInputMemoryState.remember("  draft  ");
		assertEquals("  draft  ", ChatInputMemoryState.savedText());
	}

	@Test
	void sentTextIsNotKept() {
		ChatInputMemoryState.remember("old draft");
		ChatInputMemoryState.markSubmitted("sent message");
		assertTrue(ChatInputMemoryState.consumeSubmitted("sent message"));
		assertFalse(ChatInputMemoryState.consumeSubmitted("sent message"), "submit marker must be consumed once");
	}

	@Test
	void changedInputIsKeptAfterSubmit() {
		ChatInputMemoryState.markSubmitted("sent message");
		assertFalse(ChatInputMemoryState.consumeSubmitted("new draft"));
		ChatInputMemoryState.remember("new draft");
		assertEquals("new draft", ChatInputMemoryState.savedText());
	}

	@Test
	void clearSubmittedDropsPendingMarker() {
		ChatInputMemoryState.markSubmitted("sent message");
		ChatInputMemoryState.clearSubmitted();
		assertFalse(ChatInputMemoryState.consumeSubmitted("sent message"));
	}

	@Test
	void savedIsNotTouchedBySubmitMarker() {
		ChatInputMemoryState.remember("draft");
		ChatInputMemoryState.markSubmitted("sent");
		assertTrue(ChatInputMemoryState.consumeSubmitted("sent"));
		assertEquals("draft", ChatInputMemoryState.savedText());
	}

	@Test
	void clearSubmittedKeepsSavedDraft() {
		ChatInputMemoryState.remember("draft");
		ChatInputMemoryState.markSubmitted("sent");
		ChatInputMemoryState.clearSubmitted();
		assertFalse(ChatInputMemoryState.consumeSubmitted("sent"));
		assertEquals("draft", ChatInputMemoryState.savedText());
	}

	@Test
	void submitSequenceLeavesNoDraft() {
		ChatInputMemoryState.remember("draft");
		ChatInputMemoryState.markSubmitted("hello");
		assertTrue(ChatInputMemoryState.consumeSubmitted("hello"));
		ChatInputMemoryState.remember("");
		assertEquals("", ChatInputMemoryState.savedText());
	}

	@Test
	void interceptSequenceKeepsDraft() {
		ChatInputMemoryState.remember("draft");
		ChatInputMemoryState.markSubmitted("hello");
		ChatInputMemoryState.clearSubmitted();
		ChatInputMemoryState.remember("draft");
		assertEquals("draft", ChatInputMemoryState.savedText());
	}

	@Test
	void latestSubmitMarkerWins() {
		ChatInputMemoryState.markSubmitted("first");
		ChatInputMemoryState.markSubmitted("second");
		assertTrue(ChatInputMemoryState.consumeSubmitted("second"));
	}

	@Test
	void markerMatchesExactRawText() {
		ChatInputMemoryState.markSubmitted("  hi  ");
		assertTrue(ChatInputMemoryState.consumeSubmitted("  hi  "));
	}

	@Test
	void markerDoesNotMatchTrimmedText() {
		ChatInputMemoryState.markSubmitted("  hi  ");
		assertFalse(ChatInputMemoryState.consumeSubmitted("hi"));
	}

	@Test
	void blankTextContract() {
		ChatInputMemoryState.markSubmitted("   ");
		assertTrue(ChatInputMemoryState.consumeSubmitted("   "));
		ChatInputMemoryState.remember("   ");
		assertEquals("", ChatInputMemoryState.savedText());
	}

	@Test
	void emptyStringContract() {
		assertFalse(ChatInputMemoryState.consumeSubmitted(""));
		ChatInputMemoryState.markSubmitted("");
		assertTrue(ChatInputMemoryState.consumeSubmitted(""));
	}

	@Test
	void sentTextIsNotSavedWhenScreenStaysOpen() {
		ChatInputMemoryState.beginSession();
		ChatInputMemoryState.markSubmitted("sent");
		// 发送后界面不关闭，输入框仍保留原文；随后关闭时按已发送文本消费。
		assertTrue(ChatInputMemoryState.consumeSubmitted("sent"));
		ChatInputMemoryState.remember("");
		assertEquals("", ChatInputMemoryState.savedText());
	}

	@Test
	void editedTextAfterSendBecomesDraft() {
		ChatInputMemoryState.beginSession();
		ChatInputMemoryState.markSubmitted("sent");
		assertFalse(ChatInputMemoryState.consumeSubmitted("edited"));
		ChatInputMemoryState.remember("edited");
		assertEquals("edited", ChatInputMemoryState.savedText());
	}

	@Test
	void clearedInputAfterSendLeavesNoDraft() {
		ChatInputMemoryState.beginSession();
		ChatInputMemoryState.markSubmitted("sent");
		assertFalse(ChatInputMemoryState.consumeSubmitted(""));
		ChatInputMemoryState.remember("");
		assertEquals("", ChatInputMemoryState.savedText());
	}

	@Test
	void retypedSameTextAfterEditIsKeptAsDraft() {
		ChatInputMemoryState.markSubmitted("hello");
		// 输入框内容变化（onEdited）会撤销上一轮提交标记。
		ChatInputMemoryState.clearSubmitted();
		assertFalse(ChatInputMemoryState.consumeSubmitted("hello"));
		ChatInputMemoryState.remember("hello");
		assertEquals("hello", ChatInputMemoryState.savedText());
	}

	@Test
	void resetIsIdempotent() {
		ChatInputMemoryState.reset();
		ChatInputMemoryState.reset();
		assertEquals("", ChatInputMemoryState.savedText());
		assertFalse(ChatInputMemoryState.consumeSubmitted("x"));
	}

	@Test
	void sessionStartClearsPreviousState() {
		ChatInputMemoryState.remember("old draft");
		ChatInputMemoryState.markSubmitted("old message");
		ChatInputMemoryState.beginSession();
		assertEquals("", ChatInputMemoryState.savedText());
		assertFalse(ChatInputMemoryState.consumeSubmitted("old message"));
		assertTrue(ChatInputMemoryState.isSessionActive());
	}

	@Test
	void sessionEndDropsDraftAndMarker() {
		ChatInputMemoryState.beginSession();
		ChatInputMemoryState.remember("draft");
		ChatInputMemoryState.markSubmitted("message");
		ChatInputMemoryState.endSession();
		assertEquals("", ChatInputMemoryState.savedText());
		assertFalse(ChatInputMemoryState.consumeSubmitted("message"));
		assertFalse(ChatInputMemoryState.isSessionActive());
	}

	@Test
	void sessionStartAllowsNewDraft() {
		ChatInputMemoryState.beginSession();
		ChatInputMemoryState.remember("new draft");
		assertEquals("new draft", ChatInputMemoryState.savedText());
	}

	@Test
	void prefillIsNotSavedAsDraft() {
		assertFalse(ChatInputMemoryState.shouldRemember("/", "/"));
		assertTrue(ChatInputMemoryState.shouldRemember("/help", "/"));
		assertTrue(ChatInputMemoryState.shouldRemember("hello", ""));
		assertTrue(ChatInputMemoryState.shouldRemember("hello", null));
		assertTrue(ChatInputMemoryState.shouldRemember("   ", "   "));
	}

	@Test
	void resetKeepsSessionActive() {
		ChatInputMemoryState.beginSession();
		ChatInputMemoryState.remember("draft");
		ChatInputMemoryState.reset();
		assertTrue(ChatInputMemoryState.isSessionActive());
		assertEquals("", ChatInputMemoryState.savedText());
	}

	@Test
	void resetClearsEverything() {
		ChatInputMemoryState.remember("draft");
		ChatInputMemoryState.markSubmitted("sent");
		ChatInputMemoryState.reset();
		assertEquals("", ChatInputMemoryState.savedText());
		assertFalse(ChatInputMemoryState.consumeSubmitted("sent"));
	}
}
