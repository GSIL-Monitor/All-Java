package com.d1m.wechat.controller.member;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.d1m.wechat.controller.BaseController;
import com.d1m.wechat.controller.report.MemberReport;
import com.d1m.wechat.dto.MemberDto;
import com.d1m.wechat.dto.MemberLevelDto;
import com.d1m.wechat.dto.MemberTagDto;
import com.d1m.wechat.mapper.MemberMapper;
import com.d1m.wechat.model.Member;
import com.d1m.wechat.pamametermodel.AddMemberTagModel;
import com.d1m.wechat.service.AreaInfoService;
import com.d1m.wechat.service.MemberProfileService;
import com.d1m.wechat.service.MemberService;
import com.d1m.wechat.service.impl.MemberServiceImpl;
import com.d1m.wechat.util.Message;
import com.d1m.wechat.wxsdk.user.user.JwUserAPI;
import com.d1m.wechat.wxsdk.user.user.model.Wxuser;
import com.github.pagehelper.Page;

@Controller
@RequestMapping("/member")
public class MemberController extends BaseController {

	private Logger log = LoggerFactory.getLogger(MemberController.class);

	@Autowired
	private MemberService memberService;
	
	@Autowired
	private AreaInfoService areaInfoService;
	
	@Autowired
	private MemberProfileService memberProfileService;

	@Autowired
	private MemberMapper memberMapper;

	@RequestMapping(value = "/pullwxmember.json", method = RequestMethod.GET)
	@ResponseBody
	public JSONObject pullWxMember(HttpSession session, HttpServletRequest request) {
		try {
			String nextOpenId=request.getParameter("next_openid");
			memberService.pullWxMember(getWechatId(session),nextOpenId);
			return representation(Message.MEMBER_PULL_WX_SUCCESS);
		} catch (Exception e) {
			log.error(e.getMessage());
			return wrapException(e);
		}
	}
	@RequestMapping(value = "/updateMemberInfo.json", method = RequestMethod.GET)
	@ResponseBody
	public JSONObject updateMemberInfo(HttpSession session, HttpServletRequest request) {
		try {
			JSONObject json = new JSONObject();
			json.put("resultCode", 0);
			Integer wechatId=getWechatId(session);
			boolean isNext=true;
			String msg="";
			do{
				List<MemberDto> memberDtoList=memberService.searchBySql(wechatId,"is_subscribe =1 and nickname is null and (local_head_img_url is null or local_head_img_url ='') limit 0,1");
				Date current = new Date();
				if(memberDtoList.size()>0){
					MemberDto memberDto=memberDtoList.get(0);
					Member exist = memberService.getMember(wechatId,memberDto.getId());
					log.info("updateMemberInfo openId: {}", exist.getOpenId());
					Wxuser wxuser = JwUserAPI.getWxuser(wechatId,exist.getOpenId());
					if(null!=wxuser){
						Member newMember = memberService.getMemberByWxUser(wxuser, wechatId, current);
						MemberServiceImpl.updateMemberByWxMember(exist,newMember);
						memberService.updateAll(exist);
					}else{
						msg="openid 查找不到数据:"+memberDto.getOpenId();
						isNext=false;
						log.info("updateMemberInfo 查找不到数据 openId: {}", exist.getOpenId());
					}
				}else{
					msg="获取数据为空";
					isNext=false;
				}
			}while(isNext);


			json.put("msg", msg);
			return json;
		} catch (Exception e) {
			log.error(e.getMessage());
			return wrapException(e);
		}
	}
	@RequestMapping(value = "/pullopenid2.json", method = RequestMethod.GET)
	@ResponseBody
	public JSONObject pullOpenId(HttpSession session, HttpServletRequest request) {
		try {

			Integer wechatId=getWechatId(session);
			String nextOpenId=request.getParameter("next_openid");

			boolean isNext=true;
			do{
				log.info("pullOpenId nextOpenId: {}", nextOpenId);
				HashMap<String,Object> resultMap = JwUserAPI.getOpenIdList(wechatId,nextOpenId);
				if(resultMap.containsKey("nextOpenId")){

					List<String> openIdList=(List<String>)resultMap.get("openIdList");
					List<Member> members = new ArrayList<Member>();
					String nextOpenIdValue=(String)resultMap.get("nextOpenId");
					log.info("pullOpenId nextOpenIdValue: {}", nextOpenIdValue);
					for (String openId:openIdList){
						Member member = memberService.getMemberByOpenId(wechatId,openId);
						log.info("pullOpenId findOpenId: {} wechatId {}", openId,wechatId);
						if(null==member){
							member = new Member();
							member.setActivity((byte) 5);
							member.setBatchsendMonth(0);
							member.setIsSubscribe(false);
							member.setOpenId(openId);
							member.setWechatId(wechatId);
							member.setCreatedAt(new Date());
							//members.add(member);
							memberMapper.insert(member);
							log.info("pullOpenId insertOpenId: {} wechatId {}", openId,wechatId);
						}
					}
					//memberMapper.insertList(members);
					log.info("pullOpenId next: {}", members.size());

					if(null==nextOpenIdValue||nextOpenIdValue.equals("")){
						isNext=false;
					}else{
						nextOpenId=nextOpenIdValue;
					}
				}else{
					isNext=false;
				}
			}while (isNext);
			log.info("pullOpenId finish: {}", nextOpenId);
			return representation(Message.MEMBER_PULL_WX_SUCCESS);
		} catch (Exception e) {
			log.error(e.getMessage());
			return wrapException(e);
		}
	}

	@RequestMapping(value = "{id}/get.json", method = RequestMethod.GET)
	@ResponseBody
	@RequiresPermissions("member:list")
	public JSONObject get(@PathVariable Integer id, HttpSession session) {
		try {
			
			MemberDto memberDto = memberService.getMemberDto(
					getWechatId(session), id);
			MemberLevelDto memberLevelDto = memberService.selectMemberProfile(id, getWechatId(session));
			memberDto.setCredits(memberLevelDto.getCredits());
			memberDto.setLevel(memberLevelDto.getLevel());
			Integer bindStatus = memberProfileService.getMemberBindStatus(id, getWechatId(session));
			if(bindStatus != null){
				memberDto.setBindStatus(bindStatus);
			}else{
				memberDto.setBindStatus(2);
			}
			return representation(Message.MEMBER_GET_SUCCESS, memberDto);
		} catch (Exception e) {
			log.error(e.getMessage());
			return wrapException(e);
		}
	}

	/**
	 * 
	 * @param activity
	 *            活跃度 [20,40]
	 * @param attentionStartTime
	 *            关注起始时间 2016-04-07T16:00:00.000Z
	 * @param attentionEndTime
	 *            关注结束时间 2016-05-07T16:00:00.000Z
	 * @param batchSendOfMonth
	 *            本月群发 [10,60]
	 * @param cancelStartTime
	 *            取消关注起始时间 2016-05-07T16:00:00.000Z
	 * @param cancelEndTime
	 *            取消关注结束时间 2016-06-07T16:00:00.000Z
	 * @param country
	 *            国家 {"code" : 860000, "remark" : "中国"}
	 * @param province
	 *            省份 {"code" : 860000, "remark" : "福建"}
	 * @param city
	 *            城市 {"code" : 860000, "remark" : "宁德"}
	 * @param nickname
	 *            昵称
	 * @param sex
	 *            性别 {"code" : 0, "value" : "男"}
	 * @param subscribe
	 *            是否关注 {"code" : 0, "value" : "未关注"}
	 * @param memberIds
	 *            会员ID [2,4,5,6,7]
	 * @param memberTags
	 *            标签[{id:1},{id:3}]
	 * @param sortName
	 * @param sortDir
	 * @param pageNum
	 * @param pageSize
	 * @param session
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "list.json", method = RequestMethod.POST)
	@ResponseBody
	@RequiresPermissions("member:list")
	public JSONObject list(
			@RequestBody(required = false) AddMemberTagModel addMemberTagModel,
			HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		if (addMemberTagModel == null) {
			addMemberTagModel = new AddMemberTagModel();
		}
		Page<MemberDto> page = memberService.search(getWechatId(session),
				addMemberTagModel, true);
		return representation(Message.MEMBER_LIST_SUCCESS, page.getResult(),
				addMemberTagModel.getPageNum(),
				addMemberTagModel.getPageSize(), page.getTotal());
	}
	
	@RequestMapping(value = "/export.json", method = RequestMethod.POST)
	@ResponseBody
	@RequiresPermissions("member:list")
	public ModelAndView exportExcel(
			HttpSession session, HttpServletRequest request,
			HttpServletResponse response){
		AddMemberTagModel addMemberTagModel = null;
		String data = request.getParameter("data");
		if(StringUtils.isNotBlank(data)){
			addMemberTagModel = (AddMemberTagModel) JSON.parseObject(data, AddMemberTagModel.class);
		}
		if (addMemberTagModel == null) {
			addMemberTagModel = new AddMemberTagModel();
		}
		ModelMap model = new ModelMap();
		List<MemberDto> memberDtos = null;
		Integer[] memberIds = addMemberTagModel.getMemberIds();
		Boolean sendToAll = addMemberTagModel.getSendToAll();
		if (memberIds != null && memberIds.length != 0){
			memberDtos = memberService.getMemberList(addMemberTagModel, getWechatId(session));
		}else if(sendToAll != null && addMemberTagModel.getSendToAll()){
			memberDtos = memberService.getAll(getWechatId(session));
		}else{
			memberDtos = memberService.searchAll(getWechatId(session),
				addMemberTagModel, false);
		}
		String lang = RequestContextUtils.getLocale(request).getCountry();
		for (MemberDto temp:memberDtos){
			if (temp.getCountry() != null){
				String country = areaInfoService.selectNameById(Integer.parseInt(temp.getCountry()), lang);
				temp.setCountry(country);
			}
			if (temp.getProvince() != null){
				String province = areaInfoService.selectNameById(Integer.parseInt(temp.getProvince()), lang);
				temp.setProvince(province);
			}
			if (temp.getCity() != null){
				String city = areaInfoService.selectNameById(Integer.parseInt(temp.getCity()), lang);
				temp.setCity(city);
			}
		}
		
		model.put("memberDto", memberDtos);
		MemberReport report = new MemberReport();
		return new ModelAndView(report, model);
		
	}
	
	

	/**
	 * 
	 * 
	 * memberIds为null或者空:根据条件筛选的全部会员加标签，memberIds不为空:指定的会员加标签
	 * 
	 * @param activity
	 *            活跃度 [20,40]
	 * @param attentionStartTime
	 *            关注起始时间 2016-04-07T16:00:00.000Z
	 * @param attentionEndTime
	 *            关注结束时间 2016-05-07T16:00:00.000Z
	 * @param batchSendOfMonth
	 *            本月群发 [10,60]
	 * @param cancelStartTime
	 *            取消关注起始时间 2016-05-07T16:00:00.000Z
	 * @param cancelEndTime
	 *            取消关注结束时间 2016-06-07T16:00:00.000Z
	 * @param country
	 *            国家 {"code" : 860000, "remark" : "中国"}
	 * @param province
	 *            省份 {"code" : 860000, "remark" : "福建"}
	 * @param city
	 *            城市 {"code" : 860000, "remark" : "宁德"}
	 * @param nickname
	 *            昵称
	 * @param sex
	 *            性别 {"code" : 0, "value" : "男"}
	 * @param subscribe
	 *            是否关注 {"code" : 0, "value" : "未关注"}
	 * @param memberIds
	 *            会员ID [2,4,5,6,7]
	 * @param memberTags
	 *            标签[{id:1},{id:3}]
	 * @param tags
	 *            标签(没有id则新增标签) [{id:1},{id:3},{name:"技术员"}]
	 * @param session
	 * @return
	 */
	@RequestMapping(value = "addtag.json", method = RequestMethod.POST)
	@ResponseBody
	@RequiresPermissions("member:list")
	public JSONObject addMemberTag(
			@RequestBody(required = false) AddMemberTagModel addMemberTagModel,
			HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<MemberTagDto> memberTagDtos = memberService.addMemberTag(
					getWechatId(session), getUser(session), addMemberTagModel);
			Map<String, List<MemberTagDto>> map = new HashMap<String, List<MemberTagDto>>();
			map.put("tags", memberTagDtos);
			return representation(Message.MEMBER_ADD_TAG_SUCCESS, map);
		} catch (Exception e) {
			log.error(e.getMessage());
			return wrapException(e);
		}
	}

	/**
	 * 删除某个会员单个标签
	 * @param memberId
	 * @param tagId
	 * @param session
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{memberId}/{memberTagId}/deltag.json", method = RequestMethod.DELETE)
	@ResponseBody
	public JSONObject deltag(@PathVariable Integer memberId,
			@PathVariable Integer memberTagId,
			HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			memberService.deleteMemberTag(getWechatId(session), memberId, memberTagId);
			return representation(Message.MEMBER_TAG_DELETE_SUCCESS);
		} catch (Exception e) {
			log.error(e.getMessage());
			return wrapException(e);
		}

	}
	
	@RequestMapping(value = "initwxtag.json", method = RequestMethod.GET)
	@ResponseBody
	@RequiresPermissions("member:list")
	public JSONObject syncWxTag(
			HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			memberService.syncWxTag(
					getWechatId(session), getUser(session));
			return representation(Message.MEMBER_SYNC_WX_TAG_SUCCESS);
		} catch (Exception e) {
			log.error(e.getMessage());
			return wrapException(e);
		}
	}
}
