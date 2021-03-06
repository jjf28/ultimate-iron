package org.iron.ultimate.jpa.dao.repository;

import org.iron.ultimate.jpa.dao.model.ArUserName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface ArUserNameRepository extends JpaRepository<ArUserName, Long> {

}
