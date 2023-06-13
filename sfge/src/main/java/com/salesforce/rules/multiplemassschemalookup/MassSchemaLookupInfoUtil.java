package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;

/** Utility to help with violation message creation on AvoidMassSchemaLookupRule */
public final class MassSchemaLookupInfoUtil {
    private MassSchemaLookupInfoUtil() {}

    public static String getMessage(MultipleMassSchemaLookupInfo info) {
        return getMessage(
                info.getSinkVertex().getFullMethodName(),
                info.getRepetitionType(),
                getOccurrenceInfoValue(info.getRepetitionType(), info.getRepetitionVertex()),
                info.getRepetitionVertex().getDefiningType(),
                info.getRepetitionVertex().getBeginLine());
    }

    public static String getMessage(
            String sinkMethodName,
            MmslrUtil.RepetitionType repetitionType,
            String occurrenceInfoValue,
            String occurrenceClassName,
            int occurrenceLine) {
        final String occurrenceMessage = getOccurrenceMessage(repetitionType, occurrenceInfoValue);

        return String.format(
                UserFacingMessages.MultipleMassSchemaLookupRuleTemplates.MESSAGE_TEMPLATE,
                sinkMethodName,
                occurrenceMessage,
                occurrenceClassName,
                occurrenceLine);
    }

    private static String getOccurrenceInfoValue(
            MmslrUtil.RepetitionType repetitionType, SFVertex repetitionVertex) {
        if (MmslrUtil.RepetitionType.MULTIPLE.equals(repetitionType)) {
            // Use method name on template message
            return ((MethodCallExpressionVertex) repetitionVertex).getFullMethodName();
        } else if (MmslrUtil.RepetitionType.ANOTHER_PATH.equals(repetitionType)) {
            if (repetitionVertex instanceof MethodCallExpressionVertex) {
                return ((MethodCallExpressionVertex) repetitionVertex).getFullMethodName();
            } else if (repetitionVertex instanceof NewObjectExpressionVertex) {
                return ((NewObjectExpressionVertex) repetitionVertex).getResolvedInnerClassName().orElse("new object");
            } else {
                return repetitionVertex.getLabel();
            }
        } else {
            // Use Loop type on template message
            return repetitionVertex.getLabel();
        }
    }

    private static String getOccurrenceMessage(
            MmslrUtil.RepetitionType repetitionType, String value) {
        return repetitionType.getMessage(value);
    }
}
