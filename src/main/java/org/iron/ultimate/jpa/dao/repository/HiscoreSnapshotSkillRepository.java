package org.iron.ultimate.jpa.dao.repository;

import org.iron.ultimate.jpa.dao.model.HiscoreSnapshotSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface HiscoreSnapshotSkillRepository extends JpaRepository<HiscoreSnapshotSkill, HiscoreSnapshotSkill.Pk> {

}
