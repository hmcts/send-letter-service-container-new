package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.cmc.pdf.generator.HTMLToPDFConverter;
import uk.gov.hmcts.reform.sendletter.services.pdf.IHtmlToPdfConverter;

@Configuration
public class PdfConversionConfig {

    @Bean
    public IHtmlToPdfConverter htmlToPdfConverter() {
        return new HTMLToPDFConverter()::convert;
    }
}
