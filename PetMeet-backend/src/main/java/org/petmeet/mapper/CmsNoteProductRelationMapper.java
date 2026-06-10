package org.petmeet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.petmeet.dto.NoteProductCountDTO;
import org.petmeet.entity.CmsNoteProductRelation;
import org.petmeet.entity.PmsProduct;
import java.util.List;

@Mapper
public interface CmsNoteProductRelationMapper extends BaseMapper<CmsNoteProductRelation> {

    @Select("SELECT p.* FROM pms_product p INNER JOIN cms_note_product_relation r ON p.id = r.product_id WHERE r.note_id = #{noteId} AND p.status = 1")
    List<PmsProduct> selectProductsByNoteId(@Param("noteId") Long noteId);

    @Select("SELECT r.note_id FROM cms_note_product_relation r WHERE r.product_id = #{productId}")
    List<Long> selectNoteIdsByProductId(@Param("productId") Long productId);

    @Select("""
            <script>
            SELECT r.note_id AS noteId, COUNT(1) AS productCount
            FROM cms_note_product_relation r
            WHERE r.note_id IN
            <foreach collection='noteIds' item='id' open='(' separator=',' close=')'>
              #{id}
            </foreach>
            GROUP BY r.note_id
            </script>
            """)
    List<NoteProductCountDTO> selectProductCountByNoteIds(@Param("noteIds") List<Long> noteIds);
}
