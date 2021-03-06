package com.d1m.wechat.service;

import java.util.Date;
import java.util.List;

import com.d1m.wechat.dto.*;
import com.d1m.wechat.exception.WechatException;
import com.d1m.wechat.model.Member;
import com.d1m.wechat.model.Qrcode;
import com.d1m.wechat.model.User;
import com.d1m.wechat.model.enums.MemberSource;
import com.d1m.wechat.pamametermodel.AddMemberTagModel;
import com.d1m.wechat.wxsdk.user.user.model.Wxuser;
import com.github.pagehelper.Page;

public interface MemberService extends IService<Member> {

	Page<MemberDto> search(Integer wechatId,
			AddMemberTagModel addMemberTagModel, boolean queryCount);

	MemberDto getMemberDto(Integer wechatId, Integer id);

	Member getMember(Integer wechatId, Integer id);

	Member getMemberByOpenId(Integer wechatId, String openId);

	List<MemberTagDto> addMemberTag(Integer wechatId, User user,
			AddMemberTagModel addMemberTagModel) throws WechatException;

	List<MemberDto> searchBySql(Integer wechatId, String sql);

	void pullWxMember(Integer wechatId,String nextOpenId) throws WechatException;

	Member getMemberByWxUser(Wxuser wxuser, Integer wechatId, Date current);

	TrendDto trend(Integer wechatId, Date start, Date end);

	PieDto pie(Integer wechatId, Date start, Date end, String type);

	Page<ReportActivityUserDto> activityUser(Integer wechatId, Date start,
			Date end, Integer top);

	ReportUserSourceDto sourceUser(Integer wechatId, Date start, Date end);

	List<ReportAreaBaseDto> pieArea(Integer wechatId, Date start, Date end,
			String type);

	List<MemberTagDto> getMemberMemberTag(Integer wechatId, Integer id);
	
	void deleteMemberTag(Integer wechatId, Integer memberId, Integer memberTagId);

	List<MemberDto> getMemberList(AddMemberTagModel addMemberTagModel,
			Integer wechatId);

	List<MemberDto> getAll(Integer wechatId);

	List<MemberStatusDto> getMemberStatus(Integer wechatId, Date end);

	void updateMemberActivity(MemberStatusDto memberStatusDto,Date endDate);

	List<MemberDto> searchAll(Integer wechatId,
			AddMemberTagModel addMemberTagModel, boolean queryCount);

	MemberDto selectByOpenId(String openId, Integer wechatId);

	MemberLevelDto selectMemberProfile(Integer id, Integer wechatId);

	void syncWxTag(Integer wechatId, User user);

	Member createMember(Integer wechatId,String openId, Date current);

	void updateWxuser(Member member, Qrcode qrcode, MemberSource memberSource);

}
