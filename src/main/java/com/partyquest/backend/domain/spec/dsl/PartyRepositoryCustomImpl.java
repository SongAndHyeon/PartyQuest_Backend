package com.partyquest.backend.domain.spec.dsl;

import com.partyquest.backend.domain.entity.Party;
import com.partyquest.backend.domain.type.PartyMemberType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.partyquest.backend.domain.entity.QParty.party;
import static com.partyquest.backend.domain.entity.QUserParty.userParty;
import static com.partyquest.backend.domain.entity.QUser.user;

@RequiredArgsConstructor
@Repository
public class PartyRepositoryCustomImpl implements PartyRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Party> findByTitle(String title) {
        return jpaQueryFactory.selectFrom(party).where(party.title.eq(title)).fetch();
    }

    /**
     * @author 송민규
     * <h3>파티 검색 동적 쿼리</h3>
     * <li>해당 메서드의 파라미터(Nullable)를 기반으로 검색을 수행합니다.</li>
     * <li>특정 파라미터가 NULL일 경우, 해당 키워드는 검색을 제외합니다.</li>
     * <li>모든 파라미터가 NULL일 경우, 검색 조건을 모두 제외하고, Party Entity의 isPublic이 TRUE인 Party들을 검색합니다.</li>
     * @param master String(Nullable)
     * @param title String(Nullable)
     * @return Optional
     */
    @Override
    public Optional<List<Party>> getParty(@Nullable String master,@Nullable String title) {
        return Optional.ofNullable(
                jpaQueryFactory
                    .selectFrom(party)
                    .join(party.userParties,userParty)
                    .join(userParty.user, user)
                    .where(titleEq(title),masterEq(master),party.isPublic.eq(true),party.isDelete.eq(false))
                .fetch()
        );
//        return jpaQueryFactory
//                .selectFrom(party)
//                .join(party.userParties, userParty)
//                .join(userParty.user, user)
//                .where(userParty.memberGrade.eq(PartyMemberType.MASTER), user.nickname.eq(master))
//                .fetch();
    }
    @Override
    public Optional<Party> getParty(long id) {
        return Optional.ofNullable(
                jpaQueryFactory
                        .selectFrom(party)
                        .where(party.id.eq(id))
                        .fetchOne()
        );
    }

    private BooleanExpression masterEq(String master) {
        return master != null? userParty.memberGrade.eq(PartyMemberType.MASTER).and(user.nickname.eq(master)) : null;
    }
    private BooleanExpression titleEq(String title) {
        return title != null? party.title.eq(title):null;
    }
}