package org.petmeet.dto;

import lombok.Data;
import org.petmeet.vo.NoteListVO;

import java.util.ArrayList;
import java.util.List;

@Data
public class NoteListPageCacheDTO {
    private long current;
    private long size;
    private long total;
    private List<NoteListVO> records;

    public static NoteListPageCacheDTO of(long current, long size, long total, List<NoteListVO> records) {
        NoteListPageCacheDTO dto = new NoteListPageCacheDTO();
        dto.setCurrent(current);
        dto.setSize(size);
        dto.setTotal(total);
        dto.setRecords(records == null ? new ArrayList<>() : records);
        return dto;
    }
}

