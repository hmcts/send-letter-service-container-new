package uk.gov.hmcts.reform.sendletter.services.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sendletter.exceptions.DuplexException;

import java.io.IOException;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuplexPreparatorTest {

    private static void call() {
        new DuplexPreparator().prepare("corruptedStream".getBytes());
    }

    @Test
    void should_add_blank_page_if_total_number_of_pages_is_odd() throws Exception {
        // given
        byte[] before = toByteArray(getResource("single_page.pdf"));

        // when
        byte[] after = new DuplexPreparator().prepare(before);

        // then
        assertThat(after).isNotEqualTo(before);
        try (PDDocument pdDoc = PDDocument.load(after)) {
            assertThat(pdDoc.getNumberOfPages()).as("number of pages").isEqualTo(2);
        }
    }

    @Test
    void should_not_add_a_blank_page_if_total_number_of_pages_is_even() throws Exception {
        // given
        byte[] before = toByteArray(getResource("two_pages.pdf"));

        // when
        byte[] after = new DuplexPreparator().prepare(before);

        // then
        assertThat(after).isEqualTo(before);
    }

    @Test
    void should_throw_duplex_exception_when_stream_is_not_pdf() {
        assertThatThrownBy(DuplexPreparatorTest::call)
            .isInstanceOfAny(IOException.class, DuplexException.class);
    }
}