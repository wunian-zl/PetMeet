package org.petmeet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.entity.PmsCategory;
import java.util.List;

public interface PmsCategoryService extends IService<PmsCategory> {
    List<PmsCategory> listEnabled();
}
