package org.iron.ultimate.jpa.dao.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "DIR_CLAN_RANK")
public class DirClanRank implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "RANK_NAME")
	private String rankName;
	
	@Column(name = "RANK_INDEX")
	private Integer rankIndex;

	@Column(name = "DISPLAY_NAME")
	private String displayName;
	
	@Column(name = "IS_STAFF")
	private Boolean isStaff;
	
	@Column(name = "MINIMUM_TOTAL")
	private Integer minimumTotal;
	
	@Column(name = "MINIMUM_CLAN_DAYS")
	private Integer minimumClanDays;
	
	public DirClanRank() {
		super();
	}

	public String getRankName() {
		return rankName;
	}
	public void setRankName(String rankName) {
		this.rankName = rankName;
	}
	public Integer getRankIndex() {
		return rankIndex;
	}
	public void setRankIndex(Integer rankIndex) {
		this.rankIndex = rankIndex;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public Boolean getIsStaff() {
		return isStaff;
	}
	public void setIsStaff(Boolean isStaff) {
		this.isStaff = isStaff;
	}
	public Integer getMinimumTotal() {
		return minimumTotal;
	}
	public void setMinimumTotal(Integer minimumTotal) {
		this.minimumTotal = minimumTotal;
	}
	public Integer getMinimumClanDays() {
		return minimumClanDays;
	}
	public void setMinimumClanDays(Integer minimumClanDays) {
		this.minimumClanDays = minimumClanDays;
	}

}
