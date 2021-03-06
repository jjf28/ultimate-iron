package org.iron.ultimate.jpa.dao.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "HISCORE_SNAPSHOT_SKILL")
public class HiscoreSnapshotSkill implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Embeddable
	public static class Pk implements Serializable {
		
		private static final long serialVersionUID = 1L;

		@Column(name = "HISCORE_SNAPSHOT_ID")
		private Long hiscoreSnapshotId;

		@Column(name = "SKILL_ID")
		private Long skillId;
		
		public Pk() {
			super();
		}
		
		public Long getHiscoreSnapshotId() {
			return hiscoreSnapshotId;
		}
		public void setHiscoreSnapshotId(Long hiscoreSnapshotId) {
			this.hiscoreSnapshotId = hiscoreSnapshotId;
		}
		public Long getSkillId() {
			return skillId;
		}
		public void setSkillId(Long skillId) {
			this.skillId = skillId;
		}
	}
	
	@EmbeddedId
	private Pk pk;
	
	@Column(name = "SKILL_LEVEL")
	private Long skillLevel;
	
	@Column(name = "SKILL_EXPERIENCE")
	private Long skillExperience;
	
	public HiscoreSnapshotSkill() {
		super();
	}
	
	public Pk getPk() {
		return pk;
	}
	public void setPk(Pk pk) {
		this.pk = pk;
	}
	public Long getSkillLevel() {
		return skillLevel;
	}
	public void setSkillLevel(Long skillLevel) {
		this.skillLevel = skillLevel;
	}
	public Long getSkillExperience() {
		return skillExperience;
	}
	public void setSkillExperience(Long skillExperience) {
		this.skillExperience = skillExperience;
	}
	
}
