package com.ruoyi.web.controller.business;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.Word;
import com.ruoyi.system.service.IWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 单词Controller
 *
 * @author ruoyi
 * @date 2023-12-28
 */
@RestController
@RequestMapping("/system/word")
public class WordController extends BaseController {
    @Autowired
    private IWordService wordService;

    /**
     * 查询单词列表
     */
    @PreAuthorize("@ss.hasPermi('system:word:list')")
    @GetMapping("/list")
    public TableDataInfo list(Word word) {
        startPage();
        List<Word> list = wordService.selectWordList(word);
        return getDataTable(list);
    }

    /**
     * 导出单词列表
     */
    @PreAuthorize("@ss.hasPermi('system:word:export')")
    @Log(title = "单词", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Word word) {
        List<Word> list = wordService.selectWordList(word);
        ExcelUtil<Word> util = new ExcelUtil<Word>(Word.class);
        util.exportExcel(response, list, "单词数据");
    }

    /**
     * 获取单词详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:word:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(wordService.selectWordById(id));
    }

    /**
     * 新增单词
     */
    @PreAuthorize("@ss.hasPermi('system:word:add')")
    @Log(title = "单词", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Word word) {
        return toAjax(wordService.insertWord(word));
    }

    /**
     * 修改单词
     */
    @PreAuthorize("@ss.hasPermi('system:word:edit')")
    @Log(title = "单词", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Word word) {
        return toAjax(wordService.updateWord(word));
    }

    /**
     * 删除单词
     */
    @PreAuthorize("@ss.hasPermi('system:word:remove')")
    @Log(title = "单词", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(wordService.deleteWordByIds(ids));
    }

    @Log(title = "单词", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<Word> util = new ExcelUtil<>(Word.class);
        List<Word> wordList = util.importExcel(file.getInputStream());
        String message = wordService.importWords(wordList, updateSupport, this.getUserId());
        return AjaxResult.success(message);
    }

    @GetMapping("/importTemplate")
    public AjaxResult importTemplate() {
        ExcelUtil<Word> util = new ExcelUtil<>(Word.class);
        return util.importTemplateExcel("单词数据");
    }

    @GetMapping("/getOneWord/{index}")
    public AjaxResult getOneWord(@PathVariable("index") int index) {
       return success(wordService.getOneWord(this.getUserId(), index));
    }
}
