package com.d1m.wechat.service.impl;

import com.d1m.wechat.dto.*;
import com.d1m.wechat.exception.WechatException;
import com.d1m.wechat.mapper.*;
import com.d1m.wechat.model.*;
import com.d1m.wechat.model.enums.Language;
import com.d1m.wechat.model.enums.MemberSource;
import com.d1m.wechat.model.enums.MemberTagStatus;
import com.d1m.wechat.model.enums.Sex;
import com.d1m.wechat.pamametermodel.AddMemberTagModel;
import com.d1m.wechat.pamametermodel.MemberModel;
import com.d1m.wechat.pamametermodel.MemberTagModel;
import com.d1m.wechat.service.MemberService;
import com.d1m.wechat.service.MemberTagTypeService;
import com.d1m.wechat.util.BeanUtil;
import com.d1m.wechat.util.DateUtil;
import com.d1m.wechat.util.FileUploadConfigUtil;
import com.d1m.wechat.util.Message;
import com.d1m.wechat.wxsdk.core.req.model.user.Tag;
import com.d1m.wechat.wxsdk.user.tag.JwTagApi;
import com.d1m.wechat.wxsdk.user.user.JwUserAPI;
import com.d1m.wechat.wxsdk.user.user.model.Wxuser;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.common.Mapper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.d1m.wechat.util.IllegalArgumentUtil.notBlank;

@Service
public class MemberServiceImpl extends BaseService<Member> implements
		MemberService {

	private Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);
	private static String defaultMedium = "qrcode";

	@Autowired
	private MemberMapper memberMapper;

	@Autowired
	private MemberTagMapper memberTagMapper;

	@Autowired
	private MemberMemberTagMapper memberMemberTagMapper;

	@Autowired
	private AreaInfoMapper areaInfoMapper;

	@Autowired
	private MemberTagTypeMapper memberTagTypeMapper;

	@Autowired
	private MemberProfileMapper memberProfileMapper;
	
	@Autowired
	private MemberTagTypeService memberTagTypeService;

	@Autowired
	private ConversationMapper conversationMapper;

	@Autowired
	private CampaignMemberTagMapper campaignMemberTagMapper;

	public void setMemberMapper(MemberMapper memberMapper) {
		this.memberMapper = memberMapper;
	}

	public void setMemberTagMapper(MemberTagMapper memberTagMapper) {
		this.memberTagMapper = memberTagMapper;
	}

	public void setMemberMemberTagMapper(
			MemberMemberTagMapper memberMemberTagMapper) {
		this.memberMemberTagMapper = memberMemberTagMapper;
	}

	@Override
	public Mapper<Member> getGenericMapper() {
		return memberMapper;
	}

	@Override
	public MemberDto getMemberDto(Integer wechatId, Integer id) {
		List<MemberDto> list = memberMapper.selectByMemberId(
				new Integer[] { id }, wechatId, null);

		if (!list.isEmpty()) {
			MemberDto dto = list.get(0);
			return dto;
		}
		return null;
	}

	@Override
	public Member getMember(Integer wechatId, Integer id) {
		Member record = new Member();
		record.setWechatId(wechatId);
		record.setId(id);
		return memberMapper.selectOne(record);
	}

	@Override
	public Member getMemberByOpenId(Integer wechatId, String openId) {
		Member record = new Member();
		record.setWechatId(wechatId);
		record.setOpenId(openId);
		return memberMapper.selectOne(record);
	}

	@Override
	public List<MemberTagDto> getMemberMemberTag(Integer wechatId, Integer id) {
		return memberMapper.getMemberMemberTags(wechatId, id);
	}

	@Override
	public Page<MemberDto> search(Integer wechatId,
			AddMemberTagModel addMemberTagModel, boolean queryCount) {
		if (addMemberTagModel == null) {
			addMemberTagModel = new AddMemberTagModel();
		}
		if (addMemberTagModel.pagable()) {
			PageHelper.startPage(addMemberTagModel.getPageNum(),
					addMemberTagModel.getPageSize(), false);
		}
		MemberModel memberModel = addMemberTagModel.getMemberModel();
		Page<MemberDto> list =  memberMapper.search(wechatId, memberModel.getOpenId(),
				memberModel.getNickname(), memberModel.getSex(), memberModel
						.getCountry(), memberModel.getProvince(), memberModel
						.getCity(), memberModel.getSubscribe(), memberModel
						.getActivityStartAt(), memberModel.getActivityEndAt(),
				memberModel.getBatchSendOfMonthStartAt(), memberModel
						.getBatchSendOfMonthEndAt(), DateUtil
						.getDateBegin(DateUtil.parse(memberModel
								.getAttentionStartAt())), DateUtil
						.getDateEnd(DateUtil.parse(memberModel
								.getAttentionEndAt())), DateUtil
						.getDateBegin(DateUtil.parse(memberModel
								.getCancelSubscribeStartAt())), DateUtil
						.getDateEnd(DateUtil.parse(memberModel
								.getCancelSubscribeEndAt())), memberModel
						.getIsOnline(), null, memberModel.getMobile(),
				memberModel.getMemberTags(), addMemberTagModel.getSortName(),
				addMemberTagModel.getSortDir(), addMemberTagModel
						.getBindStatus());

		long total = memberMapper.count(wechatId, memberModel.getOpenId(),
				memberModel.getNickname(), memberModel.getSex(), memberModel
						.getCountry(), memberModel.getProvince(), memberModel
						.getCity(), memberModel.getSubscribe(), memberModel
						.getActivityStartAt(), memberModel.getActivityEndAt(),
				memberModel.getBatchSendOfMonthStartAt(), memberModel
						.getBatchSendOfMonthEndAt(), DateUtil
						.getDateBegin(DateUtil.parse(memberModel
								.getAttentionStartAt())), DateUtil
						.getDateEnd(DateUtil.parse(memberModel
								.getAttentionEndAt())), DateUtil
						.getDateBegin(DateUtil.parse(memberModel
								.getCancelSubscribeStartAt())), DateUtil
						.getDateEnd(DateUtil.parse(memberModel
								.getCancelSubscribeEndAt())), memberModel
						.getIsOnline(), null, memberModel.getMobile(),
				memberModel.getMemberTags(), addMemberTagModel.getSortName(),
				addMemberTagModel.getSortDir(), addMemberTagModel
						.getBindStatus());
		list.setTotal(total);
		return list;
	}

	@Override
	public List<MemberDto> getMemberList(AddMemberTagModel addMemberTagModel,
			Integer wechatId) {
		return memberMapper.selectByMemberId(addMemberTagModel.getMemberIds(),
				wechatId, null);
	}

	@Override
	public List<MemberDto> getAll(Integer wechatId) {
		return memberMapper.selectByWechat(wechatId);
	}

	@Override
	public List<MemberDto> searchBySql(Integer wechatId, String sql) {
		return memberMapper.searchBySql(wechatId, sql);
	}

	@Override
	public List<MemberTagDto> addMemberTag(Integer wechatId, User user,
			AddMemberTagModel addMemberTagModel) throws WechatException {
		if (addMemberTagModel.emptyQuery()) {
			throw new WechatException(Message.MEMBER_NOT_BLANK);
		}

		List<MemberTag> memberTagsIn = getMemberTags(wechatId, user,
				addMemberTagModel.getTags());

		List<MemberDto> members = null;
		if (addMemberTagModel.getMemberIds() == null
				|| addMemberTagModel.getMemberIds().length == 0) {
			MemberModel memberModel = addMemberTagModel.getMemberModel();
			members = memberMapper.search(wechatId, memberModel.getOpenId(),
					memberModel.getNickname(), memberModel.getSex(),
					memberModel.getCountry(), memberModel.getProvince(),
					memberModel.getCity(), memberModel.getSubscribe(),
					memberModel.getActivityStartAt(), memberModel
							.getActivityEndAt(), memberModel
							.getBatchSendOfMonthStartAt(), memberModel
							.getBatchSendOfMonthEndAt(), DateUtil
							.getDateBegin(DateUtil.parse(memberModel
									.getAttentionStartAt())), DateUtil
							.getDateEnd(DateUtil.parse(memberModel
									.getAttentionEndAt())), DateUtil
							.getDateBegin(DateUtil.parse(memberModel
									.getCancelSubscribeStartAt())), DateUtil
							.getDateEnd(DateUtil.parse(memberModel
									.getCancelSubscribeEndAt())), null, null,
					memberModel.getMobile(), memberModel.getMemberTags(), null,
					null, null);
		} else {
			members = memberMapper.selectByMemberId(
					addMemberTagModel.getMemberIds(), wechatId, null);
		}
		if (members.isEmpty()) {
			throw new WechatException(Message.MEMBER_NOT_BLANK);
		}

		Date current = new Date();
		List<MemberMemberTag> memberMemberAddTags = null;
		List<MemberTagDto> memberMemberDeleteTags = null;
		MemberMemberTag memberMemberTag = null;
		List<MemberTagDto> existMemberTags = null;
		List<MemberTag> memberTags = null;
		for (MemberDto memberDto : members) {
			try {
				memberTags = BeanUtil.copyTo(memberTagsIn, MemberTag.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
			memberMemberAddTags = new ArrayList<MemberMemberTag>();
			memberMemberDeleteTags = new ArrayList<MemberTagDto>();
			existMemberTags = memberDto.getMemberTags();
			// 补全批量删除时用户已有标签
			for (MemberTagDto existMemberTag : existMemberTags) {
				if (!contains(memberTags, existMemberTag)) {
					MemberTag tag = new MemberTag();
					tag.setId(existMemberTag.getId());
					tag.setName(existMemberTag.getName());
					tag.setWechatId(existMemberTag.getWechatId());
					memberTags.add(tag);
				}
			}
			for (MemberTag memberTag : memberTags) {
				if (!contains(existMemberTags, memberTag)) {
					memberMemberTag = new MemberMemberTag();
					memberMemberTag.setMemberId(memberDto.getId());
					memberMemberTag.setMemberTagId(memberTag.getId());
					memberMemberTag.setWechatId(wechatId);
					memberMemberTag.setCreatedAt(current);
					memberMemberAddTags.add(memberMemberTag);
				}
			}
			for (MemberTagDto existMemberTag : existMemberTags) {
				if (!contains(memberTags, existMemberTag)) {
					memberMemberDeleteTags.add(existMemberTag);
				}
			}
			if (!memberMemberAddTags.isEmpty()) {
				memberMemberTagMapper.insertList(memberMemberAddTags);
			}
			if (!memberMemberDeleteTags.isEmpty()) {
				for (MemberTagDto memberTagDto : memberMemberDeleteTags) {
					memberMemberTagMapper.deleteByPrimaryKey(memberTagDto
							.getMemberMemberTagId());
				}
			}
		}

		List<MemberTagDto> memberTagDtos = new ArrayList<MemberTagDto>();
		MemberTagDto memberTagDto = null;
		for (MemberTag memberTag : memberTags) {
			memberTagDto = new MemberTagDto();
			memberTagDto.setId(memberTag.getId());
			memberTagDto.setName(memberTag.getName());
			memberTagDtos.add(memberTagDto);
		}
		return memberTagDtos;
	}

	private boolean contains(List<MemberTagDto> list, MemberTag memberTag) {
		if (list == null || list.isEmpty()) {
			return false;
		}
		for (MemberTagDto memberTagDto : list) {
			if (memberTagDto.getId().equals(memberTag.getId())) {
				return true;
			}
		}
		return false;
	}

	private boolean contains(List<MemberTag> list, MemberTagDto memberTagDto) {
		if (list == null || list.isEmpty()) {
			return false;
		}
		for (MemberTag memberTag : list) {
			if (memberTag.getId().equals(memberTagDto.getId())) {
				return true;
			}
		}
		return false;
	}

	private List<MemberTag> getMemberTags(Integer wechatId, User user,
			List<MemberTagModel> memberTagModels) throws WechatException {
		MemberTag memberTag = null;
		MemberTagType memberTagType = null;
		List<MemberTag> memberTags = new ArrayList<MemberTag>();
		Date current = new Date();
		for (MemberTagModel memberTagModel : memberTagModels) {
			if (memberTagModel.getId() == null
					&& StringUtils.isBlank(memberTagModel.getName())) {
				throw new WechatException(Message.MEMBER_TAG_TYPE_NOT_BLANK);
			}
			memberTag = new MemberTag();
			if (memberTagModel.getId() != null) {
				memberTag.setWechatId(wechatId);
				memberTag.setId(memberTagModel.getId());
				memberTag = memberTagMapper.selectOne(memberTag);
				notBlank(memberTag, Message.MEMBER_TAG_NOT_EXIST);
				memberTags.add(memberTag);
			} else {
				notBlank(memberTagModel.getMemberTagTypeId(),
						Message.MEMBER_TAG_TYPE_ID_NOT_EMPTY);
				memberTagType = new MemberTagType();
				memberTagType.setWechatId(wechatId);
				memberTagType.setId(memberTagModel.getMemberTagTypeId());
				memberTagType = memberTagTypeMapper.selectOne(memberTagType);
				notBlank(memberTagType, Message.MEMBER_TAG_TYPE_NOT_EXIST);
				MemberTag exist = memberTagMapper.getDuplicateName(wechatId,
						null, memberTagModel.getName());
				if (exist != null) {
					memberTags.add(exist);
				} else {
					memberTag.setWechatId(wechatId);
					memberTag.setName(memberTagModel.getName());
					memberTag.setCreatedAt(current);
					memberTag.setCreatorId(user.getId());
					memberTag.setStatus(MemberTagStatus.INUSED.getValue());
					memberTag.setWechatId(wechatId);
					memberTag.setMemberTagTypeId(memberTagModel
							.getMemberTagTypeId());
					memberTagMapper.insert(memberTag);
					memberTags.add(memberTag);
				}
			}

		}
		return memberTags;
	}

	@Override
	public void pullWxMember(Integer wechatId,String nextOpenId)
			throws WechatException {
		log.info("wechatId {} start pull wx member.nextOpenId: {}", wechatId,nextOpenId);
		//String accessToken = RefreshAccessTokenJob.getAccessTokenStr(wechatId);
		List<Wxuser> allWxuser = JwUserAPI.getAllWxuser(wechatId,nextOpenId);
		if (allWxuser.isEmpty()) {
			log.info("wechatId {} user is empty.nextOpenId: {}", wechatId,nextOpenId);
		}
		List<Member> members = new ArrayList<Member>();
		Date current = new Date();
		Member newMember = null;
		Member exist = null;
		for (Wxuser wxuser : allWxuser) {
			newMember = getMemberByWxUser(wxuser, wechatId, current);
			exist = getMemberByOpenId(wechatId, wxuser.getOpenid());
			if (exist != null) {
				log.info("member exist {}.", wxuser.getOpenid());
				updateMemberByWxMember(exist, newMember);
				log.info("nickname : {}", exist.getNickname());
				updateAll(exist);
			} else {
				log.info("member add {}.", wxuser.getOpenid());
				members.add(newMember);
			}
			if (members.size() == 100) {
				memberMapper.insertList(members);
				members = new ArrayList<Member>();
			}
		}
		if (!members.isEmpty()) {
			memberMapper.insertList(members);
		}

		/*accessToken = RefreshAccessTokenJob.getAccessTokenStr(wechatId);
		List<Tag> wxTags = JwTagApi.getAllTag(accessToken);
		Integer memberTagTypeDefaultId = memberTagTypeService.getDefaultId();
		User user = (User) SecurityUtils.getSubject().getPrincipal();

		for(Tag wxTag:wxTags){
			MemberTag record = new MemberTag();
			MemberTag memberTag = new MemberTag();
			record.setName(wxTag.getName());
			record.setStatus(MemberTagStatus.INUSED.getValue());
			record = memberTagMapper.selectOne(record);
			if(record == null){
				memberTag.setName(wxTag.getName());
				memberTag.setCreatedAt(new Date());
				memberTag.setWechatId(wechatId);
				memberTag.setCreatorId(user.getId());
				memberTag.setStatus((byte)1);
				memberTag.setMemberTagTypeId(memberTagTypeDefaultId);
				memberTagMapper.insert(memberTag);
			}

			List<String> openIdList = new ArrayList<String>();
			outter: for(Wxuser wxuser : allWxuser){
				List<Long> tagWxList = wxuser.getTagid_list();
				for(Long tagWx:tagWxList){
					if(tagWx.toString().equals(wxTag)){
						openIdList.add(wxuser.getOpenid());
						continue outter;
					}
				}
			}

			if(openIdList==null || openIdList.isEmpty()){
				continue;
			}

			List<Integer> memberIdList = memberMapper.getMemberIdsByOpenIds(openIdList);
			int size = memberIdList.size();
			Integer[] memberIds = (Integer[])memberIdList.toArray(new Integer[size]);
			AddMemberTagModel addMemberTagModel = new AddMemberTagModel();
			List<MemberTagModel> tags = new ArrayList<MemberTagModel>();

			MemberTagModel memberTagModel = new MemberTagModel();
			memberTagModel.setName(wxTag.getName());
			if(record!=null){
				memberTagModel.setId(record.getId());
				memberTagModel.setMemberTagTypeId(record.getMemberTagTypeId());
			}else{
				memberTagModel.setId(memberTag.getId());
				memberTagModel.setMemberTagTypeId(memberTag.getMemberTagTypeId());
			}
			if(memberIds.length>0){
				tags.add(memberTagModel);
				addMemberTagModel.setMemberIds(memberIds);
				addMemberTagModel.setTags(tags);
				addMemberTag(wechatId, user, addMemberTagModel);
			}

		}*/

		log.info("wechatId {} pull wx member finish. nextOpenId:{}}", wechatId,nextOpenId);
	}

	public static void updateMemberByWxMember(Member member, Member newMember) {
		member.setSubscribeAt(newMember.getSubscribeAt());
		if (newMember.getProvince() != null) {
			member.setProvince(newMember.getProvince());
		}
		if (newMember.getCity() != null) {
			member.setCity(newMember.getCity());
		}
		if (newMember.getCountry() != null) {
			member.setCountry(newMember.getCountry());
		}
		if (StringUtils.isNotBlank(newMember.getLocalHeadImgUrl())) {
			member.setLocalHeadImgUrl(newMember.getLocalHeadImgUrl());
		}
		if (StringUtils.isNotBlank(newMember.getHeadImgUrl())) {
			member.setHeadImgUrl(newMember.getHeadImgUrl());
		}
		if (newMember.getLanguage() != null) {
			member.setLanguage(newMember.getLanguage());
		}
		if (StringUtils.isNotBlank(newMember.getNickname())) {
			member.setNickname(newMember.getNickname());
		}
		if (StringUtils.isNotBlank(newMember.getRemark())) {
			member.setRemark(newMember.getRemark());
		}
		if (newMember.getSex() != null) {
			member.setSex(newMember.getSex());
		} else {
			member.setSex((byte)0);
		}
		if (StringUtils.isNotBlank(newMember.getUnionId())) {
			member.setUnionId(newMember.getUnionId());
		}
		member.setSubscribeAt(newMember.getSubscribeAt());
		member.setIsSubscribe(newMember.getIsSubscribe());
	}

	@Override
	public Member getMemberByWxUser(Wxuser wxuser, Integer wechatId,
			Date current) {

		Member member = new Member();
		if(1==wxuser.getSubscribe()){
			AreaInfo areaInfo = getAreaInfo(null, wxuser.getCountry());
			if (areaInfo != null) {
				member.setCountry(areaInfo.getId());
			}
			areaInfo = getAreaInfo((areaInfo != null ? areaInfo.getId() : null),
					wxuser.getProvince());
			if (areaInfo != null) {
				member.setProvince(areaInfo.getId());
			}
			areaInfo = getAreaInfo((areaInfo != null ? areaInfo.getId() : null),
					wxuser.getCity());
			if (areaInfo != null) {
				member.setCity(areaInfo.getId());
			}

			Language language = Language.getValueByName(wxuser.getLanguage());
			if (language != null) {
				member.setLanguage(language.getValue());
			}
			member.setNickname(wxuser.getNickname());
			member.setRemark(wxuser.getRemark());
			Sex sex = Sex.getByValue(Byte.parseByte(wxuser.getSex()));
			if (sex != null) {
				member.setSex(sex.getValue());
			}
			member.setUnionId(wxuser.getUnionid());
			member.setSubscribeAt(DateUtil.formatWxTime(wxuser.getSubscribe_time()));
		}else{
			member.setNickname("");
		}
		if (StringUtils.isBlank(wxuser.getHeadimgurl())) {
			String memberDefaultAvatarPath = FileUploadConfigUtil.getInstance()
					.getValue("member_default_avatar_path");
			if (StringUtils.isNotBlank(memberDefaultAvatarPath)) {
				member.setLocalHeadImgUrl(memberDefaultAvatarPath);
			}
		} else {
			member.setHeadImgUrl(wxuser.getHeadimgurl());
			member.setLocalHeadImgUrl(wxuser.getHeadimgurl());
			/*
			String format = DateUtil.yyyyMMddHHmmss.format(new Date());
			String type = Constants.IMAGE + File.separator + Constants.NORMAL;
			File root = FileUtils.getUploadPathRoot(type);
			File dir = new File(root, format.substring(0, 6));
			String suffix = FileUtils.getRemoteImageSuffix(
					wxuser.getHeadimgurl(), Constants.JPG);
			String newFileName = FileUtils.generateNewFileName(
					MD5.MD5Encode(""), suffix);
			FileUtils.copyRemoteFile(wxuser.getHeadimgurl(),
					dir.getAbsolutePath(), newFileName);
			member.setLocalHeadImgUrl(FileUploadConfigUtil.getInstance()
					.getValue("upload_url_base")
					+ File.separator
					+ type
					+ File.separator
					+ dir.getName()
					+ File.separator
					+ newFileName);
			*/
		}
		member.setCreatedAt(current);
		member.setOpenId(wxuser.getOpenid());
		member.setWechatId(wechatId);
		member.setIsSubscribe(wxuser.getSubscribe() == 1 ? true : false);
		return member;
	}

	private AreaInfo getAreaInfo(Integer parentId, String name) {
		AreaInfo areaInfo = new AreaInfo();
		if (parentId != null) {
			areaInfo.setParentId(parentId);
		}
		if(StringUtils.isBlank(name)){
			return null;
		}
		areaInfo.setcName(name);
		areaInfo = areaInfoMapper.selectOne(areaInfo);
		return areaInfo;
	}

	private List<TrendBaseDto> findDates(Date begin, Date end) {
		List<TrendBaseDto> list = new ArrayList<TrendBaseDto>();
		TrendBaseDto dto = new TrendBaseDto();
		dto.setTime(DateUtil.formatYYYYMMDD(begin));
		dto.setCount(0);
		list.add(dto);

		Calendar calBegin = Calendar.getInstance();
		calBegin.setTime(begin);
		calBegin.set(Calendar.HOUR_OF_DAY, 0);
		calBegin.set(Calendar.MINUTE, 0);
		calBegin.set(Calendar.SECOND, 0);
		calBegin.set(Calendar.MILLISECOND, 0);
		Calendar calEnd = Calendar.getInstance();
		calEnd.setTime(end);
		calEnd.set(Calendar.HOUR_OF_DAY, 0);
		calEnd.set(Calendar.MINUTE, 0);
		calEnd.set(Calendar.SECOND, 0);
		calEnd.set(Calendar.MILLISECOND, 0);
		while (calEnd.after(calBegin)) {
			calBegin.add(Calendar.DAY_OF_MONTH, 1);
			dto = new TrendBaseDto();
			dto.setTime(DateUtil.formatYYYYMMDD(calBegin.getTime()));
			dto.setCount(0);
			list.add(dto);
		}
		return list;
	}

	@Override
	public TrendDto trend(Integer wechatId, Date start, Date end) {
		// TODO Auto-generated method stub
		notBlank(wechatId, Message.REPORT_ARGS_INVALID);
		notBlank(start, Message.REPORT_ARGS_INVALID);
		notBlank(end, Message.REPORT_ARGS_INVALID);
		Integer total = memberMapper.trendReportTotal(wechatId, end);

		List<TrendBaseDto> attentionList = findDates(start, end);
		List<TrendBaseDto> attentionResult = memberMapper.trendReportAttention(
				wechatId, start, end);
		List<TrendBaseDto> attentionTimesList = findDates(start, end);
		List<TrendBaseDto> attentionTimesResult = memberMapper.trendReportAttentionTimes(
				wechatId, start, end);
		for (TrendBaseDto dto : attentionList) {
			for (TrendBaseDto temp : attentionResult) {
				if (dto.getTime().equals(temp.getTime())) {
					dto.setCount(temp.getCount());
					break;
				}
			}
		}
		for (TrendBaseDto dto : attentionTimesList) {
			for (TrendBaseDto temp : attentionTimesResult) {
				if (dto.getTime().equals(temp.getTime())) {
					dto.setCount(temp.getCount());
					break;
				}
			}
		}

		List<TrendBaseDto> cancelList = findDates(start, end);
		List<TrendBaseDto> cancelResult = memberMapper.trendReportCancel(
				wechatId, start, end);
		List<TrendBaseDto> cancelTimesList = findDates(start, end);
		List<TrendBaseDto> cancelTimesResult = memberMapper.trendReportCancelTimes(
				wechatId, start, end);
		for (TrendBaseDto dto : cancelList) {
			for (TrendBaseDto temp : cancelResult) {
				if (dto.getTime().equals(temp.getTime())) {
					dto.setCount(temp.getCount());
					break;
				}
			}
		}
		for (TrendBaseDto dto : cancelTimesList) {
			for (TrendBaseDto temp : cancelTimesResult) {
				if (dto.getTime().equals(temp.getTime())) {
					dto.setCount(temp.getCount());
					break;
				}
			}
		}

		TrendDto dto = new TrendDto();
		dto.setAttentionList(attentionList);
		dto.setCancelList(cancelList);
		dto.setAttentionTimesList(attentionTimesList);
		dto.setCancelTimesList(cancelTimesList);
		int attention = 0;
		int cancel = 0;
		int attentionTimes = 0;
		int cancelTimes = 0;
		
		if (attentionList != null && attentionList.size() > 0) {
			for (int i = 0; i < attentionList.size(); i++) {
				attention += attentionList.get(i).getCount();
			}
		}
		dto.setAttention(attention);

		if (cancelList != null && cancelList.size() > 0) {
			for (int i = 0; i < cancelList.size(); i++) {
				cancel += cancelList.get(i).getCount();
			}
		}
		dto.setCancel(cancel);
		
		if (attentionTimesList != null && attentionTimesList.size() > 0) {
			for (int i = 0; i < attentionTimesList.size(); i++) {
				attentionTimes += attentionTimesList.get(i).getCount();
			}
		}
		dto.setAttentionTimes(attentionTimes);
		
		if (cancelTimesList != null && cancelTimesList.size() > 0) {
			for (int i = 0; i < cancelTimesList.size(); i++) {
				cancelTimes += cancelTimesList.get(i).getCount();
			}
		}
		dto.setCancelTimes(cancelTimes);

		dto.setTotal(total);
		if (total > 0) {
			dto.setNewRate((double) attention / dto.getTotal());
			dto.setCancleRate((double) cancel / dto.getTotal());
		}

		int bind = memberProfileMapper.getBindNumber(wechatId, start, end);
		int unBind = memberProfileMapper.getUnBindNumber(wechatId, start, end);
		dto.setBind(bind);
		dto.setUnBind(unBind);

		return dto;
	}

	@Override
	public PieDto pie(Integer wechatId, Date start, Date end, String type) {
		// TODO Auto-generated method stub
		notBlank(wechatId, Message.REPORT_ARGS_INVALID);
		notBlank(start, Message.REPORT_ARGS_INVALID);
		notBlank(end, Message.REPORT_ARGS_INVALID);
		notBlank(type, Message.REPORT_ARGS_INVALID);

		PieDto pieDto = new PieDto();
		String sqlType = type;
		if ("gender".equalsIgnoreCase(type)) {
			sqlType = "sex";
		}
		List<PieBaseDto> result = memberMapper.pieReport(wechatId, start, end,
				sqlType);
		if (result != null && result.size() > 0) {
			if (type.contains("gender")) {// 性别
				int total = 0;
				for (PieBaseDto data : result) {
					total += data.getCount();
				}
				for (PieBaseDto data : result) {
					data.setRate((double) data.getCount() / total);
				}
			} else if (type.contains("language")) {// 语言
				int total = 0;
				for (PieBaseDto data : result) {
					total += data.getCount();
				}
				for (PieBaseDto data : result) {
					data.setRate((double) data.getCount() / total);
				}
			}
			pieDto.setList(result);
		}
		pieDto.setType(type);
		return pieDto;
	}

	@Override
	public Page<ReportActivityUserDto> activityUser(Integer wechatId,
			Date start, Date end, Integer top) {
		// TODO Auto-generated method stub
		notBlank(wechatId, Message.REPORT_ARGS_INVALID);
		notBlank(start, Message.REPORT_ARGS_INVALID);
		notBlank(end, Message.REPORT_ARGS_INVALID);
		PageHelper.startPage(1, top, true);
		return memberMapper.activityUser(wechatId, start, end, top);
	}

	private List<ReportUserSourceDto> findDates4UserSource(Date begin, Date end) {
		List<ReportUserSourceDto> list = new ArrayList<ReportUserSourceDto>();
		ReportUserSourceDto dto = new ReportUserSourceDto();
		dto.setDate(DateUtil.formatYYYYMMDD(begin));
		list.add(dto);

		Calendar calBegin = Calendar.getInstance();
		calBegin.setTime(begin);
		calBegin.set(Calendar.HOUR_OF_DAY, 0);
		calBegin.set(Calendar.MINUTE, 0);
		calBegin.set(Calendar.SECOND, 0);
		calBegin.set(Calendar.MILLISECOND, 0);
		Calendar calEnd = Calendar.getInstance();
		calEnd.setTime(end);
		calEnd.set(Calendar.HOUR_OF_DAY, 0);
		calEnd.set(Calendar.MINUTE, 0);
		calEnd.set(Calendar.SECOND, 0);
		calEnd.set(Calendar.MILLISECOND, 0);
		while (calEnd.after(calBegin)) {
			calBegin.add(Calendar.DAY_OF_MONTH, 1);
			dto = new ReportUserSourceDto();
			dto.setDate(DateUtil.formatYYYYMMDD(calBegin.getTime()));
			list.add(dto);
		}
		return list;
	}

	@Override
	public ReportUserSourceDto sourceUser(Integer wechatId, Date start, Date end) {
		// TODO Auto-generated method stub
		notBlank(wechatId, Message.REPORT_ARGS_INVALID);
		notBlank(start, Message.REPORT_ARGS_INVALID);
		notBlank(end, Message.REPORT_ARGS_INVALID);

		List<ReportUserSourceDto> allList = findDates4UserSource(start, end);
		List<ReportUserSourceDto> list = memberMapper.sourceUser(wechatId,
				start, end);
		for (ReportUserSourceDto dto : allList) {
			for (ReportUserSourceDto temp : list) {
				if (dto.getDate().equals(temp.getDate())) {
					dto.setBusinessCard(temp.getBusinessCard());
					dto.setMomentsAd(temp.getMomentsAd());
					dto.setOther(temp.getOther());
					dto.setPayAttentionAfter(temp.getPayAttentionAfter());
					dto.setQrCode(temp.getQrCode());
					dto.setTopRightMenu(temp.getTopRightMenu());
					dto.setWechatInArticleAd(temp.getWechatInArticleAd());
					dto.setWechatInImageText(temp.getWechatInImageText());
					dto.setWechatSearch(temp.getWechatSearch());
					break;
				}
			}
		}
		ReportUserSourceDto dto = new ReportUserSourceDto();

		if (allList != null && allList.size() > 0) {
			dto.setList(allList);
			for (ReportUserSourceDto param : allList) {
				dto.setBusinessCard(dto.getBusinessCard()
						+ param.getBusinessCard());
				dto.setWechatSearch(dto.getWechatSearch()
						+ param.getWechatSearch());
				dto.setQrCode(dto.getQrCode() + param.getQrCode());
				dto.setTopRightMenu(dto.getTopRightMenu()
						+ param.getTopRightMenu());
				dto.setWechatInImageText(dto.getWechatInImageText()
						+ param.getWechatInImageText());
				dto.setWechatInArticleAd(dto.getWechatInArticleAd()
						+ param.getWechatInArticleAd());
				dto.setMomentsAd(dto.getMomentsAd() + param.getMomentsAd());
				dto.setPayAttentionAfter(dto.getPayAttentionAfter()
						+ param.getPayAttentionAfter());
				dto.setOther(dto.getOther() + param.getOther());
			}
		}
		return dto;
	}

	@Override
	public List<ReportAreaBaseDto> pieArea(Integer wechatId, Date start,
			Date end, String type) {
		// TODO Auto-generated method stub
		notBlank(wechatId, Message.REPORT_ARGS_INVALID);
		notBlank(start, Message.REPORT_ARGS_INVALID);
		notBlank(end, Message.REPORT_ARGS_INVALID);
		notBlank(type, Message.REPORT_ARGS_INVALID);

		List<ReportAreaBaseDto> country = memberMapper.pieAreaReport(wechatId,
				start, end, type);
		if (country != null && country.size() > 0) {
			for (ReportAreaBaseDto dto : country) {
				if (dto != null) {
					List<ReportAreaBaseDto> provice = memberMapper.getProvice(
							wechatId, start, end, dto.getId());
					if (provice != null && provice.size() > 0) {
						for (ReportAreaBaseDto proviceDto : provice) {
							if (proviceDto != null) {
								List<ReportAreaBaseDto> city = memberMapper
										.getCity(wechatId, start, end,
												dto.getId(), proviceDto.getId());
								proviceDto.setCity(city);
							}
						}
					}
					dto.setProvince(provice);
				}
			}
		}

		return country;
	}

	@Override
	public void deleteMemberTag(Integer wechatId, Integer memberId,
			Integer memberTagId) {
		memberMapper.deleteMemberTag(wechatId, memberId, memberTagId);
	}
	public List<MemberStatusDto> getMemberStatus(Integer wechatId, Date end){
		List<MemberStatusDto> list = memberMapper.memberStatus(wechatId, end);
		return list;
	}

	@Override
	public void updateMemberActivity(MemberStatusDto memberStatusDto,Date endDate) {
		Integer memberId = null;
		Boolean isSubscribe = null;
		Date menuClickLast = null;
		Date conversationLast = null;
		int days = 0;
		isSubscribe = memberStatusDto.getIsSubscribe();
		memberId = memberStatusDto.getMemberId();
		menuClickLast = memberStatusDto.getMenuClickLast();
		conversationLast = memberStatusDto.getConversationLast();

		Member member = memberMapper.selectByPrimaryKey(memberId);
		byte activity = 0;
		double weight = 0;
		if (isSubscribe) {
			activity = 5;
			// 计算活跃度 - 菜单点击维度
			if (menuClickLast != null) {
				days = DateUtil.daysBetween(menuClickLast, endDate);
				weight = 1 - days * 0.1;
				weight = weight < 0 ? 0 : weight;
				activity = (byte) (activity + 15 * weight);
			}
			// 计算活跃度 - 客服聊天维度
			if (conversationLast != null) {
				days = DateUtil.daysBetween(conversationLast, endDate);
				weight = 1 - days * 0.1;
				weight = weight < 0 ? 0 : weight;
				activity = (byte) (activity + 20 * weight);
			}
			member.setActivity(activity);
		} else {
			// 活跃度清零
			member.setActivity((byte) 0);
		}
		memberMapper.updateByPrimaryKey(member);
	}

	@Override
	public List<MemberDto> searchAll(Integer wechatId,
			AddMemberTagModel addMemberTagModel, boolean queryCount) {
		if (addMemberTagModel == null) {
			addMemberTagModel = new AddMemberTagModel();
		}
		addMemberTagModel.setPageSize(0);
		if (addMemberTagModel.pagable()) {
			PageHelper.startPage(addMemberTagModel.getPageNum(),
					addMemberTagModel.getPageSize(), queryCount, null, true);
		}
		MemberModel memberModel = addMemberTagModel.getMemberModel();
		return memberMapper.search(wechatId, memberModel.getOpenId(),
				memberModel.getNickname(), memberModel.getSex(), memberModel
						.getCountry(), memberModel.getProvince(), memberModel
						.getCity(), memberModel.getSubscribe(), memberModel
						.getActivityStartAt(), memberModel.getActivityEndAt(),
				memberModel.getBatchSendOfMonthStartAt(), memberModel
						.getBatchSendOfMonthEndAt(), DateUtil
						.getDateBegin(DateUtil.parse(memberModel
								.getAttentionStartAt())), DateUtil
						.getDateEnd(DateUtil.parse(memberModel
								.getAttentionEndAt())), DateUtil
						.getDateBegin(DateUtil.parse(memberModel
								.getCancelSubscribeStartAt())), DateUtil
						.getDateEnd(DateUtil.parse(memberModel
								.getCancelSubscribeEndAt())), memberModel
						.getIsOnline(), null, memberModel.getMobile(),
				memberModel.getMemberTags(), addMemberTagModel.getSortName(),
				addMemberTagModel.getSortDir(), addMemberTagModel
						.getBindStatus());
	}

	@Override
	public MemberDto selectByOpenId(String openId, Integer wechatId) {
		return memberMapper.selectByOpenId(openId, wechatId);
	}

	@Override
	public MemberLevelDto selectMemberProfile(Integer id, Integer wechatId) {
		MemberLevelDto memberLevelDto = memberMapper.selectMemberProfile(id,
				wechatId);
		if (memberLevelDto == null) {
			memberLevelDto = new MemberLevelDto();
		}
		if (memberLevelDto.getCredits() == null) {
			memberLevelDto.setCredits(0);
		}
		if(memberLevelDto.getLevel() == null){
			memberLevelDto.setLevel("V1");
		}
		return memberLevelDto;
	}

	@Override
	public synchronized void syncWxTag(Integer wechatId, User user) {
		log.info("sync wechat {} start pull wx member tag.", wechatId);
		String accessToken = RefreshAccessTokenJob.getAccessTokenStr(wechatId);
		List<Tag> wxTags = JwTagApi.getAllTag(accessToken);
		Integer memberTagTypeDefaultId = memberTagTypeService.getDefaultId();
		for(Tag wxTag:wxTags){
			String accesstoken = RefreshAccessTokenJob.getAccessTokenStr(wechatId);
			List<String> openIdList = JwTagApi.getUserByTag(accesstoken, wxTag.getId(), "");
			if(openIdList == null){
				MemberTag record1 = new MemberTag();
				MemberTag memberTag1 = new MemberTag();
				record1.setName(wxTag.getName());
				record1 = memberTagMapper.selectOne(record1);
				if(record1 == null){
					memberTag1.setName(wxTag.getName());
					memberTag1.setCreatedAt(new Date());
					memberTag1.setWechatId(wechatId);
					memberTag1.setCreatorId(user.getId());
					memberTag1.setStatus((byte)1);
					memberTag1.setMemberTagTypeId(memberTagTypeDefaultId);
					memberTagMapper.insert(memberTag1);
				}
				continue;
			}
			
			List<List<String>> openIdListArr = new ArrayList<List<String>>();  
	        int arrSize = openIdList.size()%100==0?openIdList.size()/100:openIdList.size()/100+1;  
	        for(int i=0;i<arrSize;i++) {  
	            List<String> sub = new ArrayList<String>();  
	            for(int j=i*100; j<=100*(i+1)-1; j++) {  
	                if(j<=openIdList.size()-1) {  
	                    sub.add(openIdList.get(j));  
	                }  
	            }  
	            openIdListArr.add(sub);
	        }
			for(List<String> openIdArr:openIdListArr){
				List<Integer> memberIdList = memberMapper.getMemberIdsByOpenIds(openIdArr);
				if(memberIdList == null||memberIdList.size()==0){
					continue;
				}
				int size = memberIdList.size();
				Integer[] memberIds = (Integer[])memberIdList.toArray(new Integer[size]);
				AddMemberTagModel addMemberTagModel = new AddMemberTagModel();
				List<MemberTagModel> tags = new ArrayList<MemberTagModel>();
				
				MemberTag record = new MemberTag();
				MemberTag memberTag = new MemberTag();
				record.setName(wxTag.getName());
				record.setStatus(MemberTagStatus.INUSED.getValue());
				record = memberTagMapper.selectOne(record);
				if(record == null){
					memberTag.setName(wxTag.getName());
					memberTag.setCreatedAt(new Date());
					memberTag.setWechatId(wechatId);
					memberTag.setCreatorId(user.getId());
					memberTag.setStatus(MemberTagStatus.INUSED.getValue());
					memberTag.setMemberTagTypeId(memberTagTypeDefaultId);
					memberTagMapper.insert(memberTag);
				}
				
				MemberTagModel memberTagModel = new MemberTagModel();
				memberTagModel.setName(wxTag.getName());
				if(record!=null){
					memberTagModel.setId(record.getId());
					memberTagModel.setMemberTagTypeId(record.getMemberTagTypeId());
				}else{
					memberTagModel.setId(memberTag.getId());
					memberTagModel.setMemberTagTypeId(memberTag.getMemberTagTypeId());
				}
				tags.add(memberTagModel);
				addMemberTagModel.setMemberIds(memberIds);
				addMemberTagModel.setTags(tags);
				addMemberTag(wechatId, user, addMemberTagModel);
			}
		}
		
	}

	@Override
	public Member createMember(Integer wechatId,String openId, Date current){
		Member member = getMemberByOpenId(wechatId,openId);
		if(member==null){
			synchronized(this){
				member = new Member();
				String memberDefaultAvatarPath = FileUploadConfigUtil.getInstance().getValue("member_default_avatar_path");
				if (StringUtils.isNotBlank(memberDefaultAvatarPath)) {
					member.setLocalHeadImgUrl(memberDefaultAvatarPath);
				}
				if(current==null){
					current = new Date();
				}
				member.setOpenId(openId);
				member.setIsSubscribe(true);
				member.setLastConversationAt(current);
				member.setWechatId(wechatId);
				member.setActivity((byte) 5);
				member.setBatchsendMonth(0);
				member.setCreatedAt(current);
				save(member);
			}
		} else {
			if(!member.getIsSubscribe()){
                Member update = new Member();
                update.setId(member.getId());
                update.setIsSubscribe(true);
				updateNotNull(update);
			}
		}

		return member;
	}

	/**
	 * 加了Async注解, 将此方法提交到spring管理的executor异步执行
	 */
	@Async("callerRunsExecutor")
	public void updateWxuser(Member member, Qrcode qrcode, MemberSource memberSource){
		log.info("=====updateWxuser=====");
		Integer wechatId = member.getWechatId();
		String accessToken = RefreshAccessTokenJob.getAccessTokenStr(wechatId);
		Wxuser wxuser = JwUserAPI.getWxuser(accessToken,member.getOpenId());
		Member newMember = getMemberByWxUser(wxuser, wechatId,new Date());

		member.setIsSubscribe(true);
		member.setFromwhere(memberSource.getValue());
		if (qrcode != null && StringUtils.isBlank(member.getSource()) && member.getUnsubscribeAt()==null) {
			member.setSource(qrcode.getScene());
		}
		MemberServiceImpl.updateMemberByWxMember(member, newMember);
		log.info("SubscribeAt {}", member.getSubscribeAt());
		updateAll(member);

		// 更新会话头像
		conversationMapper.updateMemberPhoto(member.getId(),member.getLocalHeadImgUrl());

		log.info("sync member tag");
		resumeWxMemberTag(wechatId, member, accessToken);

		log.info("update member.");
	}

	public void resumeWxMemberTag(Integer wechatId, Member member, String accessToken){
		CampaignMemberTag query = new CampaignMemberTag();
		query.setMemberId(member.getId());
		List<CampaignMemberTag> campaignMemberTags = campaignMemberTagMapper.select(query);
		if(campaignMemberTags == null){
			return;
		}
		for(CampaignMemberTag campaignMemberTag : campaignMemberTags){
			Integer tagId = campaignMemberTag.getWxTagId();
			List<String> openIdList = new ArrayList<>();
			openIdList.add(member.getOpenId());
			JwTagApi.memberBatchTag(accessToken, tagId.toString(), openIdList);
		}
	}

	@Override
	public int updateAll(Member entity) {
		return super.updateAll(entity);
	}

	@Override
	public int updateNotNull(Member entity) {
		return super.updateNotNull(entity);
	}
}
