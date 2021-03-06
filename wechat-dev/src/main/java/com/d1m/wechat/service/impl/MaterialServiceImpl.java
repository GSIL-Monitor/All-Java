package com.d1m.wechat.service.impl;

import static com.d1m.wechat.util.IllegalArgumentUtil.notBlank;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tk.mybatis.mapper.common.Mapper;

import com.d1m.wechat.controller.file.Upload;
import com.d1m.wechat.dto.ImageTextDto;
import com.d1m.wechat.dto.MaterialDto;
import com.d1m.wechat.exception.WechatException;
import com.d1m.wechat.mapper.MaterialImageTextDetailMapper;
import com.d1m.wechat.mapper.MaterialMapper;
import com.d1m.wechat.model.Material;
import com.d1m.wechat.model.MaterialImageTextDetail;
import com.d1m.wechat.model.Member;
import com.d1m.wechat.model.User;
import com.d1m.wechat.model.enums.MaterialStatus;
import com.d1m.wechat.model.enums.MaterialType;
import com.d1m.wechat.pamametermodel.ConversationModel;
import com.d1m.wechat.pamametermodel.ImageModel;
import com.d1m.wechat.pamametermodel.ImageTextModel;
import com.d1m.wechat.pamametermodel.MaterialModel;
import com.d1m.wechat.service.ConversationService;
import com.d1m.wechat.service.MaterialService;
import com.d1m.wechat.service.MemberService;
import com.d1m.wechat.util.HtmlUtils;
import com.d1m.wechat.util.Message;
import com.d1m.wechat.util.ParamUtil;
import com.d1m.wechat.util.RunningEnvUtil;
import com.d1m.wechat.wxsdk.core.sendmsg.JwSendMessageAPI;
import com.d1m.wechat.wxsdk.core.sendmsg.model.SendMessageResponse;
import com.d1m.wechat.wxsdk.core.sendmsg.model.WxArticle;
import com.d1m.wechat.wxsdk.core.sendmsg.model.WxArticlesResponse;
import com.d1m.wechat.wxsdk.wxbase.wxmedia.JwMediaAPI;
import com.d1m.wechat.wxsdk.wxbase.wxmedia.model.WxMediaForMaterial;
import com.d1m.wechat.wxsdk.wxbase.wxmedia.model.WxMediaForMaterialResponse;
import com.d1m.wechat.wxsdk.wxbase.wxmedia.model.WxUpload;
import com.d1m.wechat.wxsdk.wxbase.wxmedia.model.WxUploadImg;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

@Service
public class MaterialServiceImpl extends BaseService<Material> implements
		MaterialService {

	@Autowired
	private MaterialMapper materialMapper;

	@Autowired
	private MaterialImageTextDetailMapper materialImageTextDetailMapper;

	@Autowired
	private MemberService memberService;
	
	@Autowired
	private ConversationService conversationService;

	public void setMaterialMapper(MaterialMapper materialMapper) {
		this.materialMapper = materialMapper;
	}

	public void setMaterialImageTextDetailMapper(
			MaterialImageTextDetailMapper materialImageTextDetailMapper) {
		this.materialImageTextDetailMapper = materialImageTextDetailMapper;
	}

	@Override
	public MaterialDto getImageText(Integer wechatId, Integer id) {
		return materialMapper.getImageText(wechatId, id);
	}

	@Override
	public Material createImage(Integer wechatId, User user, Upload upload)
			throws WechatException {
		notBlank(upload.getAccessPath(), Message.MATERIAL_IMAGE_NOT_BLANK);
		Date current = new Date();
		Material material = createMaterialImage(wechatId, user, upload, current);

		WxUpload wxUpload = JwMediaAPI.uploadMedia(
				RefreshAccessTokenJob.getAccessTokenStr(wechatId),
				MaterialType.IMAGE.name().toLowerCase(),
				upload.getAbsolutePath());

		material.setMediaId(wxUpload.getMedia_id());
		material.setLastPushAt(current);
		materialMapper.insert(material);
		return material;
	}

	@Override
	public Material createMediaImage(Integer wechatId, User user, Upload upload) {
		notBlank(upload.getAccessPath(), Message.MATERIAL_IMAGE_NOT_BLANK);
		Date current = new Date();
		Material material = createMediaImage(wechatId, user, upload, current,
				MaterialType.MEDIAIMAGE.getValue());

		WxUploadImg wxUploadImg = JwMediaAPI.uploadMediaImg(RefreshAccessTokenJob.getAccessTokenStr(wechatId),
				upload.getAbsolutePath());
		if (wxUploadImg == null) {
			throw new WechatException(Message.SYSTEM_ERROR);
		}
		material.setWxPicUrl(wxUploadImg.getUrl());
		material.setLastPushAt(current);
		materialMapper.insert(material);
		return material;
	}

	private Material createMediaImage(Integer wechatId, User user,
			Upload upload, Date current, Byte materialType) {
		Material material = new Material();
		material.setCreatedAt(current);
		material.setCreatorId(user.getId());
		material.setMaterialType(materialType);
		material.setName(upload.getFileName());
		material.setPicUrl(upload.getAccessPath());
		material.setStatus(MaterialStatus.INUSED.getValue());
		material.setWechatId(wechatId);
		return material;
	}

	/**
	 * 暂时先注释门店图片上传
	 * @param wechatId
	 * @param user
	 * @param upload
     * @return
     */
	@Override
	public Material createOutletImage(Integer wechatId, User user, Upload upload) {
		notBlank(upload.getAccessPath(), Message.MATERIAL_IMAGE_NOT_BLANK);
		Date current = new Date();
		Material material = createMediaImage(wechatId, user, upload, current,
				MaterialType.OUTLETIMAGE.getValue());

//		WxUploadImg wxUploadImg = JwShopAPI.uploadImg(RefreshAccessTokenJob.getAccessTokenStr(wechatId),
//				upload.getAbsolutePath());
//		if (wxUploadImg == null) {
//			throw new WechatException(Message.BUSINESS_WEIXIN_PHOTO_UPLOAD_FAIL);
//		}
//		material.setWxPicUrl(wxUploadImg.getUrl());
		material.setLastPushAt(current);
		material.setModifyAt(current);
		materialMapper.insert(material);
		return material;
	}

	@Override
	public Material createMaterialImage(Integer wechatId, User user,
			Upload upload) throws WechatException {
		notBlank(upload.getAccessPath(), Message.MATERIAL_IMAGE_NOT_BLANK);
		Date current = new Date();
		Material material = createMaterialImage(wechatId, user, upload, current);
		WxMediaForMaterial wx = new WxMediaForMaterial();
		wx.setFileName(upload.getNewFileName());
		wx.setFilePath(upload.getAbsolutePath()
				.substring(
						0,
						upload.getAbsolutePath().lastIndexOf(
								java.io.File.separator) + 1));
		wx.setType(MaterialType.IMAGE.name().toLowerCase());
		
		Boolean isProd=true;
		//TODO 此处根据环境判断,如果是上线前的部署环境,不上传到微信服务器,后面通过定时任务处理
		if(RunningEnvUtil.getRunningEnv() == RunningEnvUtil.ENV_PRODUCT){
			WxMediaForMaterialResponse res = JwMediaAPI.uploadMediaFileByMaterial(
					RefreshAccessTokenJob.getAccessTokenStr(wechatId), wx);
			if (res == null) {
				throw new WechatException(Message.SYSTEM_ERROR);
			}
			material.setMediaId(res.getMedia_id());
			material.setWxPicUrl(res.getUrl());
		}

		material.setLastPushAt(current);
		materialMapper.insert(material);
		return material;
	}

	private Material createMaterialImage(Integer wechatId, User user,
			Upload upload, Date current) {
		Material material = new Material();
		material.setCreatedAt(current);
		material.setCreatorId(user.getId());
		material.setMaterialType(MaterialType.IMAGE.getValue());
		material.setName(upload.getFileName());
		material.setPicUrl(upload.getAccessPath());
		material.setStatus(MaterialStatus.INUSED.getValue());
		material.setWechatId(wechatId);
		return material;
	}

	@Override
	public Material createMaterialVideo(Integer wechatId, User user,
			Upload upload) throws WechatException {
		notBlank(upload.getAccessPath(), Message.MATERIAL_IMAGE_NOT_BLANK);
		Material material = new Material();
		material.setCreatedAt(new Date());
		material.setCreatorId(user.getId());
		material.setMaterialType(MaterialType.VIDEO.getValue());
		material.setName(upload.getAccessPath().substring(
				upload.getAccessPath().lastIndexOf("_") + 1,
				upload.getAccessPath().lastIndexOf(".")));
		material.setVideoUrl(upload.getAccessPath());
		material.setStatus(MaterialStatus.INUSED.getValue());
		material.setWechatId(wechatId);

		WxUpload wxUpload = JwMediaAPI.uploadMedia(
				RefreshAccessTokenJob.getAccessTokenStr(wechatId),
				MaterialType.IMAGE.name().toLowerCase(),
				upload.getAbsolutePath());

		material.setMediaId(wxUpload.getMedia_id());
		materialMapper.insert(material);
		return material;
	}

	@Override
	public Material createImageText(Integer wechatId, User user,
			MaterialModel materialModel) throws WechatException {
		List<ImageTextModel> imageTexts = materialModel.getImagetexts();
		if (imageTexts == null || imageTexts.isEmpty()) {
			throw new WechatException(Message.MATERIAL_IMAGE_TEXT_NOT_BLANK);
		}
		Date current = new Date();
		List<MaterialImageTextDetail> materialImageTextDetails = getMaterialImageTextDetails(
				wechatId, null, imageTexts, current);
		Material material = new Material();
		material.setCreatedAt(current);
		material.setCreatorId(user.getId());
		material.setModifyAt(current);
		material.setModifyById(user.getId());
		material.setMaterialType(MaterialType.IMAGE_TEXT.getValue());
		material.setStatus(MaterialStatus.INUSED.getValue());
		material.setWechatId(wechatId);
		materialMapper.insert(material);
		for (MaterialImageTextDetail materialImageTextDetail : materialImageTextDetails) {
			materialImageTextDetail.setMaterialId(material.getId());
			materialImageTextDetail.setStatus((byte) 1);
		}
		if (!materialImageTextDetails.isEmpty()) {
			materialImageTextDetailMapper.insertList(materialImageTextDetails);
		}
		return material;
	}

	@Override
	public void deleteImage(Integer wechatId, User user, Integer id)
			throws WechatException {
		deleteImageText(wechatId, user, id);
	}

	@Override
	public void deleteImageText(Integer wechatId, User user, Integer id)
			throws WechatException {
		notBlank(id, Message.MATERIAL_ID_NOT_BLANK);
		MaterialDto materialDto = materialMapper.getImageText(wechatId, id);
		Material material = new Material();
		material.setWechatId(wechatId);
		material.setId(id);
		material.setStatus((byte) 1);
		material = materialMapper.selectOne(material);
		notBlank(material, Message.MATERIAL_NOT_EXIST);
		material.setStatus((byte) 0);
		int res = materialMapper.updateByPrimaryKey(material);
		// 删除微信上的永久素材
		if (res > 0) {
			if (StringUtils.isNotBlank(material.getMediaId())) {
				String accessToken = RefreshAccessTokenJob
						.getAccessTokenStr(wechatId);
				JwMediaAPI.deleteArticlesByMaterial(accessToken,
						material.getMediaId());
			}
		}
		
		//set disabled status to all imageTexts of this material		
		if(materialDto != null) {
			List<ImageTextDto> items = materialDto.getItems();
			for(ImageTextDto imageText : items) {
				MaterialImageTextDetail imageTextData = new MaterialImageTextDetail();
				imageTextData.setWechatId(wechatId);
				imageTextData.setId(imageText.getId());
				imageTextData.setStatus((byte) 1);
				imageTextData = materialImageTextDetailMapper.selectOne(imageTextData);
				notBlank(imageTextData, Message.MATERIAL_NOT_EXIST);
				imageTextData.setStatus((byte) 0);
				materialImageTextDetailMapper.updateByPrimaryKey(imageTextData);
			}
		}
	}

	@Override
	public void deleteImageTextDetail(Integer wechatId, User user, Integer id)
			throws WechatException {
		notBlank(id, Message.MATERIAL_IMAGE_TEXT_DETAIL_ID_NOT_BLANK);
		MaterialImageTextDetail detail = new MaterialImageTextDetail();
		detail.setWechatId(wechatId);
		detail.setId(id);
		detail.setStatus((byte) 1);
		detail = materialImageTextDetailMapper.selectOne(detail);
		notBlank(detail, Message.MATERIAL_IMAGE_TEXT_DETAIL_NOT_EXIST);
		detail.setStatus((byte) 0);
		// TODO inused??

		materialImageTextDetailMapper.updateByPrimaryKey(detail);
	}

	@Override
	public void renameImage(Integer wechatId, User user, Integer id, String name)
			throws WechatException {
		notBlank(name, Message.MATERIAL_IMAGE_NAME_NOT_BLANK);
		Material material = getMaterial(wechatId, id);
		notBlank(material, Message.MATERIAL_NOT_EXIST);
		material.setName(name);
		materialMapper.updateByPrimaryKey(material);
	}

	@Override
	public MaterialImageTextDetail updateImageTextDetail(Integer wechatId,
			User user, Integer imageTextDetailId,
			MaterialImageTextDetail materialImageTextDetail, boolean pushToWx)
			throws WechatException {
		notBlank(imageTextDetailId,
				Message.MATERIAL_IMAGE_TEXT_DETAIL_ID_NOT_BLANK);
		notBlank(materialImageTextDetail,
				Message.MATERIAL_IMAGE_TEXT_DETAIL_ID_NOT_BLANK);
		notBlank(materialImageTextDetail.getTitle(),
				Message.MATERIAL_IMAGE_TEXT_DETAIL_TITLE_NOT_BLANK);
		notBlank(materialImageTextDetail.getContent(),
				Message.MATERIAL_IMAGE_TEXT_DETAIL_CONTENT_NOT_BLANK);
		notBlank(materialImageTextDetail.getMaterialCoverId(),
				Message.MATERIAL_IMAGE_TEXT_DETAIL_THUMB_MEDIA_NOT_BLANK);
		Material materialCover = new Material();
		materialCover.setId(materialImageTextDetail.getMaterialCoverId());
		materialCover.setWechatId(wechatId);
		materialCover.setMaterialType(MaterialType.IMAGE.getValue());
		materialCover = materialMapper.selectOne(materialCover);
		notBlank(materialCover, Message.MATERIAL_IMAGE_NOT_EXIST);
		if (materialImageTextDetail.getContentSourceChecked()) {
			notBlank(
					materialImageTextDetail.getContentSourceUrl(),
					Message.MATERIAL_IMAGE_TEXT_DETAIL_CONTENT_SOURCE_URL_NOT_BLANK);
		}

		MaterialImageTextDetail record = new MaterialImageTextDetail();
		record.setId(imageTextDetailId);
		record.setWechatId(wechatId);
		record = materialImageTextDetailMapper.selectOne(record);
		notBlank(record, Message.MATERIAL_IMAGE_TEXT_DETAIL_NOT_BLANK);
		record.setAuthor(materialImageTextDetail.getAuthor());
		record.setContent(materialImageTextDetail.getContent());
		record.setContentSourceChecked(materialImageTextDetail
				.getContentSourceChecked());
		record.setContentSourceUrl(materialImageTextDetail
				.getContentSourceUrl());
		record.setMaterialCoverId(materialImageTextDetail.getMaterialCoverId());
		record.setShowCover(materialImageTextDetail.getShowCover());
		record.setSummary(StringUtils.isBlank(materialImageTextDetail
				.getSummary()) ? StringUtils.substring(
				materialImageTextDetail.getContent(), 0, 54)
				: materialImageTextDetail.getSummary());
		record.setTitle(materialImageTextDetail.getTitle());
		materialImageTextDetailMapper.updateByPrimaryKey(record);
		// Material material = getMaterial(wechatId, record.getMaterialId());
		// if (pushToWx && StringUtils.isNotBlank(material.getMediaId())) {
		// MaterialDto imageText = getImageText(wechatId, material.getId());
		// WxUpdateArticle wxUpdateArticle = new WxUpdateArticle();
		// wxUpdateArticle.setArticle(article);
		// wxUpdateArticle.setIndex(index);
		// wxUpdateArticle.setMedia_id(media_id);

		// JwMediaAPI
		// .updateArticlesByMaterial(
		// AccessTokenThread.getAccessToken(wechatId)
		// .getAccessToken(), wxUpdateArticle);
		// }
		return record;
	}

	@Override
	public Material updateImageText(Integer wechatId, User user, Integer id,
			MaterialModel materialModel) throws WechatException {
		Material material = getMaterial(wechatId, id);
		notBlank(material, Message.MATERIAL_NOT_EXIST);
		List<ImageTextModel> imagetexts = materialModel.getImagetexts();
		if (imagetexts == null || imagetexts.isEmpty()) {
			throw new WechatException(Message.MATERIAL_IMAGE_TEXT_NOT_BLANK);
		}
		Date current = new Date();
		List<MaterialImageTextDetail> materialImageTextDetails = getMaterialImageTextDetails(
				wechatId, id, imagetexts, current);
		MaterialImageTextDetail detail = new MaterialImageTextDetail();
		detail.setWechatId(wechatId);
		detail.setStatus((byte)1);
		detail.setMaterialId(id);
		List<MaterialImageTextDetail> existDetails = materialImageTextDetailMapper
				.select(detail);
		List<MaterialImageTextDetail> deleteList = new ArrayList<MaterialImageTextDetail>();
		List<MaterialImageTextDetail> addList = new ArrayList<MaterialImageTextDetail>();
		for (MaterialImageTextDetail materialImageTextDetail : existDetails) {
			if (!contains(materialImageTextDetails, materialImageTextDetail)) {
				deleteList.add(materialImageTextDetail);
			}
		}
		for (MaterialImageTextDetail materialImageTextDetail : materialImageTextDetails) {
			if (!contains(existDetails, materialImageTextDetail)) {
				addList.add(materialImageTextDetail);
				continue;
			}
			detail = materialImageTextDetailMapper
					.selectByPrimaryKey(materialImageTextDetail.getId());
			detail.setAuthor(materialImageTextDetail.getAuthor());
			detail.setContent(materialImageTextDetail.getContent());
			detail.setContentSourceChecked(materialImageTextDetail
					.getContentSourceChecked());
			detail.setContentSourceUrl(materialImageTextDetail
					.getContentSourceUrl());
			detail.setShowCover(materialImageTextDetail.getShowCover());
			detail.setSummary(materialImageTextDetail.getSummary());
			detail.setTitle(materialImageTextDetail.getTitle());
			detail.setSequence(materialImageTextDetail.getSequence());
			detail.setMaterialCoverId(materialImageTextDetail
					.getMaterialCoverId());
			materialImageTextDetailMapper.updateByPrimaryKey(detail);
		}
		for (MaterialImageTextDetail materialImageTextDetail : deleteList) {
			materialImageTextDetail.setStatus((byte) 0);
			materialImageTextDetailMapper
					.updateByPrimaryKey(materialImageTextDetail);
		}
		for (MaterialImageTextDetail materialImageTextDetail : addList) {
			materialImageTextDetail.setStatus((byte) 1);
			materialImageTextDetail.setMaterialId(id);
		}
		if (!addList.isEmpty()) {
			materialImageTextDetailMapper.insertList(addList);
		}
		material.setModifyAt(current);
		material.setModifyById(user.getId());
		materialMapper.updateByPrimaryKey(material);
		return material;
	}

	public Material getMaterial(Integer wechatId, Integer id) {
		Material material = new Material();
		material.setWechatId(wechatId);
		material.setId(id);
		material.setStatus(MaterialStatus.INUSED.getValue());
		material = materialMapper.selectOne(material);
		return material;
	}

	private boolean contains(List<MaterialImageTextDetail> list,
			MaterialImageTextDetail detail) {
		if (detail == null) {
			return false;
		}
		for (MaterialImageTextDetail materialImageTextDetail : list) {
			if (null!=materialImageTextDetail.getId()&&materialImageTextDetail.getId().equals(detail.getId())) {
				return true;
			}
		}
		return false;
	}

	private List<MaterialImageTextDetail> getMaterialImageTextDetails(
			Integer wechatId, Integer materialId,
			List<ImageTextModel> imageTexts, Date current)
			throws WechatException {
		List<MaterialImageTextDetail> materialImageTextDetails = new ArrayList<MaterialImageTextDetail>();
		MaterialImageTextDetail detail, existDetail = null;
		String title, author, content, contentSourceUrl, summary = null;
		Integer materialCoverId = null;
		Boolean showCover, contentSourceUrlChecked = null;
		Material materialCover = null;
		Integer materialImageTextDetailId = null;
		int sequence = 1;
		for (ImageTextModel imageTextModel : imageTexts) {
			title = imageTextModel.getTitle();
			content = imageTextModel.getContent();
			materialCoverId = imageTextModel.getMaterialCoverId();
			notBlank(title, Message.MATERIAL_IMAGE_TEXT_DETAIL_TITLE_NOT_BLANK);
			notBlank(content,
					Message.MATERIAL_IMAGE_TEXT_DETAIL_CONTENT_NOT_BLANK);
			notBlank(materialCoverId,
					Message.MATERIAL_IMAGE_TEXT_DETAIL_THUMB_MEDIA_NOT_BLANK);
			materialCover = new Material();
			materialCover.setId(materialCoverId);
			materialCover.setWechatId(wechatId);
			materialCover.setMaterialType(MaterialType.IMAGE.getValue());
			materialCover.setStatus(MaterialStatus.INUSED.getValue());
			materialCover = materialMapper.selectOne(materialCover);
			notBlank(materialCover, Message.MATERIAL_IMAGE_NOT_EXIST);
			author = imageTextModel.getAuthor();
			contentSourceUrlChecked = ParamUtil.getBoolean(
					imageTextModel.getContentSourceChecked(), false);
			contentSourceUrl = imageTextModel.getContentSourceUrl();
			summary = imageTextModel.getSummary();
			showCover = imageTextModel.isShowCover();
			if (contentSourceUrlChecked) {
				notBlank(
						contentSourceUrl,
						Message.MATERIAL_IMAGE_TEXT_DETAIL_CONTENT_SOURCE_URL_NOT_BLANK);
			}
			detail = new MaterialImageTextDetail();
			detail.setAuthor(author);
			detail.setContent(content);
			detail.setContentSourceChecked(contentSourceUrlChecked);
			detail.setContentSourceUrl(contentSourceUrl);
			detail.setShowCover(showCover);
			detail.setMaterialCoverId(materialCoverId);
			if (StringUtils.isBlank(summary)) {
				String delHTMLTagContent = HtmlUtils.delHTMLTag(content);
				if (delHTMLTagContent.length() > 54) {
					delHTMLTagContent = delHTMLTagContent.substring(0, 54);
				}
				detail.setSummary(delHTMLTagContent);
			} else {
				detail.setSummary(summary);
			}
			detail.setTitle(title);
			detail.setWechatId(wechatId);
			if (materialId != null) {
				materialImageTextDetailId = imageTextModel.getId();
				if (materialImageTextDetailId != null) {
					existDetail = materialImageTextDetailMapper
							.selectByPrimaryKey(materialImageTextDetailId);
					if (!existDetail.getMaterialId().equals(materialId)) {
						throw new WechatException(
								Message.MATERIAL_IMAGE_TEXT_DETAIL_NOT_BELONGS_TO_MATERIAL);
					}
					detail.setId(materialImageTextDetailId);
				}
			}
			detail.setSequence(sequence);
			materialImageTextDetails.add(detail);
			sequence++;
		}
		return materialImageTextDetails;
	}

	@Override
	public Page<MaterialDto> searchImageText(Integer wechatId,
			ImageTextModel imageTextModel, boolean queryCount) {
		if (imageTextModel == null) {
			imageTextModel = new ImageTextModel();
		}
		PageHelper.startPage(imageTextModel.getPageNum(),
				imageTextModel.getPageSize(), queryCount);
		return materialMapper.searchImageText(wechatId,
				imageTextModel.getQuery(), imageTextModel.getPushed());
	}

	@Override
	public Page<MaterialDto> searchImage(Integer wechatId,
			ImageModel imageModel, boolean queryCount) {
		if (imageModel == null) {
			imageModel = new ImageModel();
		}
		PageHelper.startPage(imageModel.getPageNum(), imageModel.getPageSize(),
				queryCount);
		return materialMapper.searchImage(wechatId,
				imageModel.getMaterialImageTypeId(), imageModel.getQuery(),
				imageModel.getPushed(), imageModel.getMaterialType());
	}

	@Override
	public Page<MaterialDto> searchVoice(Integer wechatId,
			Integer materialType, String sortName, String sortDir,
			Integer pageNum, Integer pageSize, boolean queryCount) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<MaterialDto> searchVideo(Integer wechatId,
			Integer materialType, String sortName, String sortDir,
			Integer pageNum, Integer pageSize, boolean queryCount) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<MaterialDto> searchLittleVideo(Integer wechatId,
			Integer materialType, String sortName, String sortDir,
			Integer pageNum, Integer pageSize, boolean queryCount) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Mapper<Material> getGenericMapper() {
		return materialMapper;
	}

	@Override
	public synchronized void pushMaterialImageTextToWx(Integer wechatId,
			User user, Integer materialId) {
		MaterialDto imageText = getImageText(wechatId, materialId);
		notBlank(imageText, Message.MATERIAL_NOT_EXIST);
		if (imageText.getItems() == null || imageText.getItems().isEmpty()) {
			throw new WechatException(Message.MATERIAL_IMAGE_TEXT_NOT_BLANK);
		}
		Material material = getMaterial(wechatId, materialId);
		String accessToken = RefreshAccessTokenJob.getAccessTokenStr(wechatId);
		if (StringUtils.isNotBlank(material.getMediaId())) {
			JwMediaAPI.deleteArticlesByMaterial(accessToken,
					material.getMediaId());
		}
		List<WxArticle> wxArticles = new ArrayList<WxArticle>();
		WxArticle wxArticle = null;
		List<ImageTextDto> items = imageText.getItems();

		String patternString="(?i)<img(.*?)src=\"(.*?)\"\\s*data-wx-src=\"(.*?)\"(.*?)>";
		String backgroundPatternString="(.*?)url\\(&quot;(.*?)&quot;(.*?)background-wx-src=\"(.*?)\"(.*?)";
		for (ImageTextDto imageTextDto : items) {
			wxArticle = new WxArticle();
			wxArticle.setAuthor(imageTextDto.getAuthor());
			String textContent=imageTextDto.getContent();
			textContent=textContent.replaceAll(patternString,"<img $1 src=\"$3\" data-wx-src=\"$2\"$4>");
			textContent=textContent.replaceAll(backgroundPatternString, "$1url\\(&quot;$4&quot;$3background-wx-src=\"$2\"$5");
			wxArticle.setContent(textContent);
			wxArticle.setContent_source_url(imageTextDto.getContentSourceUrl());
			wxArticle.setDigest(imageTextDto.getSummary());
			wxArticle.setShow_cover_pic(imageTextDto.isShowCover() ? "1" : "0");
			wxArticle.setThumb_media_id(imageTextDto.getMaterialCoverMediaId());
			wxArticle.setTitle(imageTextDto.getTitle());
			wxArticles.add(wxArticle);
		}
		WxArticlesResponse wxArticlesResponse = JwMediaAPI
				.uploadArticlesByMaterial(accessToken, wxArticles);
		material.setMediaId(wxArticlesResponse.getMedia_id());
		Date current = new Date();
		material.setLastPushAt(current);
		material.setModifyAt(current);
		material.setModifyById(user.getId());
		materialMapper.updateByPrimaryKey(material);
	}

	@Override
	public void checkMaterialInvalidInWeiXin(Integer wechatId, String mediaId) {
		boolean exist = JwMediaAPI.getMaterialExist(
				RefreshAccessTokenJob.getAccessTokenStr(wechatId), mediaId);
		if (!exist) {
			throw new WechatException(Message.MATERIAL_NOT_EXIST_IN_WX);
		}
	}

	@Override
	public void previewMaterial(Integer wechatId, MaterialModel materialModel) {
		Integer id = materialModel.getId();
		notBlank(id, Message.MATERIAL_ID_NOT_BLANK);
		notBlank(materialModel.getMemberId(), Message.MEMBER_ID_NOT_EMPTY);
		Member member = memberService.getMember(wechatId,
				materialModel.getMemberId());
		notBlank(member, Message.MEMBER_NOT_EXIST);

		String accessToken = RefreshAccessTokenJob.getAccessTokenStr(wechatId);
		Material material = getMaterial(wechatId, id);
		if (material.getLastPushAt() == null
				|| material.getModifyAt() == null
				|| material.getLastPushAt().compareTo(material.getModifyAt()) != 0
				|| StringUtils.isBlank(material.getMediaId())) {
			ConversationModel conversationModel = new ConversationModel();
			conversationModel.setMaterialId(materialModel.getId());
			conversationModel.setMemberId(materialModel.getMemberId());
			User user = (User) SecurityUtils.getSubject().getPrincipal();
			conversationService.wechatToMember(wechatId, user, conversationModel);
			return;
		}
		accessToken = RefreshAccessTokenJob.getAccessTokenStr(wechatId);
		SendMessageResponse sendMessageResponse = JwSendMessageAPI
				.messagePreview(accessToken, member.getOpenId(),
						material.getMediaId());
		if (!StringUtils.equals(sendMessageResponse.getErrcode(), "0")) {
			throw new WechatException(Message.WEIXIN_HTTPS_REQUEST_ERROR);
		}
	}
}
