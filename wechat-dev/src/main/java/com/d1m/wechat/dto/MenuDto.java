package com.d1m.wechat.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MenuDto {

	private Integer id;

	private Byte type;

	private Byte materialType;

	private String name;

	private String url;

	private MaterialDto material;

	private MenuDto parent;

	private List<MenuDto> children;

	private Integer seq;

	private String appId;

	private String pagePath;

	private String appUrl;

//	public List<MenuDto> getChildren() {
//		return children;
//	}
//
//	public Integer getId() {
//		return id;
//	}
//
//	public MaterialDto getMaterial() {
//		return material;
//	}
//
//	public Byte getMaterialType() {
//		return materialType;
//	}
//
//	public String getName() {
//		return name;
//	}
//
//	public MenuDto getParent() {
//		return parent;
//	}
//
//	public Integer getSeq() {
//		return seq;
//	}
//
//	public Byte getType() {
//		return type;
//	}
//
//	public String getUrl() {
//		return url;
//	}
//
//	public void setChildren(List<MenuDto> children) {
//		this.children = children;
//	}
//
//	public void setId(Integer id) {
//		this.id = id;
//	}
//
//	public void setMaterial(MaterialDto material) {
//		this.material = material;
//	}
//
//	public void setMaterialType(Byte materialType) {
//		this.materialType = materialType;
//	}
//
//	public void setName(String name) {
//		this.name = name;
//	}
//
//	public void setParent(MenuDto parent) {
//		this.parent = parent;
//	}
//
//	public void setSeq(Integer seq) {
//		this.seq = seq;
//	}
//
//	public void setType(Byte type) {
//		this.type = type;
//	}
//
//	public void setUrl(String url) {
//		this.url = url;
//	}

}
