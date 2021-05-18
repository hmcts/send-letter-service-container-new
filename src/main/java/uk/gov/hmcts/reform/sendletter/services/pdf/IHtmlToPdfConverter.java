package uk.gov.hmcts.reform.sendletter.services.pdf;

import java.util.Map;
import java.util.function.BiFunction;

public interface IHtmlToPdfConverter extends BiFunction<byte[], Map<String, Object>, byte[]> {

}
