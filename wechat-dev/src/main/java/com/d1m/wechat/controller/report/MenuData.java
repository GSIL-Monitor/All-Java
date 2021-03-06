package com.d1m.wechat.controller.report;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.document.AbstractExcelView;

import com.d1m.wechat.dto.ReportMenuBaseDto;
import com.d1m.wechat.dto.ReportMenuDto;
import com.d1m.wechat.util.I18nUtil;

public class MenuData extends AbstractExcelView{

	@Override
	protected void buildExcelDocument(Map<String, Object> model,
			HSSFWorkbook workbook, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ReportMenuDto data = (ReportMenuDto)model.get("dto");
		List<ReportMenuBaseDto> dtos = data.getMenuList();
		Locale locale = RequestContextUtils.getLocale(request);
		String name = I18nUtil.getMessage("report.menu", locale);
		HSSFSheet sheet = workbook.createSheet(name);
		sheet.setDefaultColumnWidth(6);
		HSSFRow title = sheet.createRow(0);
		String[] keys = {"no", "report.parent.menu", "report.sub.menu", "report.menu.click.amount", 
				"report.menu.click.follower.amount", "report.menu.click.amount.per.follower"};
		String[] titleVal = I18nUtil.getMessage(keys, locale);
		for (int i = 0; i < titleVal.length; i++) {
			title.createCell(i).setCellValue(titleVal[i]);
		}

		if(dtos!=null) {
			for(ReportMenuBaseDto dto : dtos){
				if(dto.getParentName()==null){
					dto.setParentName(dto.getName());
					dto.setName(null);
				}
			}
			int rowId = 1;
			for (int i = 0; i < dtos.size(); i++) {
				ReportMenuBaseDto dto = dtos.get(i);
				HSSFRow dataRow = sheet.createRow(rowId);
				dataRow.setHeight((short) 300);
				dataRow.createCell(0).setCellValue(rowId);
				dataRow.createCell(1).setCellValue(dto.getParentName());
				dataRow.createCell(2).setCellValue(dto.getName());
				dataRow.createCell(3).setCellValue(dto.getCount());
				dataRow.createCell(4).setCellValue(dto.getUserCount());
				dataRow.createCell(5).setCellValue(dto.getPeruser());
				rowId++;
			}
		}
		
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		String date = df.format(new Date());
		String filename = name + "_" + date + ".xls";

		if (request.getHeader("User-Agent").toLowerCase().indexOf("firefox") > 0) {
			filename = new String(filename.toString().getBytes("utf-8"),
					"iso-8859-1");
		} else {
			filename = URLEncoder.encode(filename.toString(), "UTF-8");
		}
		response.setContentType("application/vnd.ms-excel");
		response.setHeader("Content-disposition", "attachment;filename="
				+ filename);
		OutputStream ouputStream = response.getOutputStream();
		workbook.write(ouputStream);
		ouputStream.flush();
		ouputStream.close();
	}
	

}
