package org.marakas73.controller.filescanner.mapper;

import org.marakas73.common.util.IntervalWrapper;
import org.marakas73.controller.filescanner.dto.request.FileScanFilterDto;
import org.marakas73.model.FileScanFilter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class FileScanFilterMapper {
    public FileScanFilter toModel(FileScanFilterDto dto) {
        IntervalWrapper<Long> sizeInBytesInterval =
                dto.sizeInBytesInterval() == null ? null : new IntervalWrapper<>(
                        dto.sizeInBytesInterval().getStart(),
                        dto.sizeInBytesInterval().getEnd()
                );

        IntervalWrapper<LocalDate> lastModifiedDateInterval =
                dto.lastModifiedDateInterval() == null ? null : new IntervalWrapper<>(
                        dto.lastModifiedDateInterval().getStart() == null ? null : LocalDate.parse(
                                dto.lastModifiedDateInterval().getStart()),
                        dto.lastModifiedDateInterval().getEnd() == null ? null : LocalDate.parse(
                                dto.lastModifiedDateInterval().getEnd())
                );

        IntervalWrapper<LocalTime> lastModifiedTimeInterval =
                dto.lastModifiedTimeInterval() == null ? null : new IntervalWrapper<>(
                        dto.lastModifiedTimeInterval().getStart() == null ? null : LocalTime.parse(
                                dto.lastModifiedTimeInterval().getStart()),
                        dto.lastModifiedTimeInterval().getEnd() == null ? null : LocalTime.parse(
                                dto.lastModifiedTimeInterval().getEnd())
                );

        return new FileScanFilter(
                dto.namePattern(),
                sizeInBytesInterval,
                lastModifiedDateInterval,
                lastModifiedTimeInterval,
                dto.textContent()
        );
    }
}
