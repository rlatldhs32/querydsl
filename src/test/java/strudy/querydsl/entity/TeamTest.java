package strudy.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import strudy.querydsl.dto.MemberDto;
import strudy.querydsl.dto.QMemberDto;

import javax.persistence.Column;
import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static strudy.querydsl.entity.QMember.*;
import static strudy.querydsl.entity.QTeam.*;


@SpringBootTest
@Transactional
//@Commit
class TeamTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;
    //    @Test
    @BeforeEach
    public void testEntity(){
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1",10,teamA);
        Member member2 = new Member("member2",40,teamA);
        Member member3 = new Member("member3",20,teamB);
        Member member4 = new Member("member4",30,teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        //
        em.flush();
        em.clear();

    }

    @Test
    public void startJPQL(){
        Member singleResult = em.createQuery("select m from Member m where m.username= :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(singleResult.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){
        Member findMember =queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

       assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("search")
    public void search() throws Exception {
        Member member1 = queryFactory.selectFrom(member)
                .where(
                        member.username.eq("member1").and(member.age.eq(10))
                )
                .fetchOne();

        assertThat(member1.getUsername()).isEqualTo("member1");
        //given
        //when
        //then

    }

    @Test
    void resultFetch() throws Exception {
        //given
        List<Member> fetch = queryFactory.selectFrom(member)
                .fetch();

        Member member1 = queryFactory.selectFrom(member).fetchOne();
        //when
        Member member2 = queryFactory.selectFrom(member).fetchFirst();

        QueryResults<Member> memberQueryResults = queryFactory.selectFrom(member).fetchResults();

        memberQueryResults.getTotal();
        memberQueryResults.getResults();
    }
    @Test
    void sort() throws Exception {
        //given
//?????? ??????????????? ???????????? ???????????? , ??? 2?????? ??????????????? ????????? ???????????? ?????? (nulls last)
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        result.get(0);
        //when
        //then
    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }


    @Test
    public void paging2(){
        QueryResults<Member> memberQueryResults = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(memberQueryResults.getTotal()).isEqualTo(4);
        assertThat(memberQueryResults.getLimit()).isEqualTo(2);
assertThat(memberQueryResults.getOffset()).isEqualTo(1);
assertThat(memberQueryResults.getResults().size()).isEqualTo(2);

    }

    @Test
    @DisplayName("")
    void agrregation() throws Exception {
        //given
        List<Tuple> result = queryFactory.select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);


        //when
        //then
    }

    @Test
    //?????? ????????? ??? ?????? ??????
    void group() throws Exception {
        //given
        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)    //????????? ?????? ??????, ?????? ??????
                .groupBy(team.name)         //team??? ???????????? grouping.
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
//        assertThat(team)
        //when

        //then
    }

    /**
     * team A??? ????????? ?????? ??????
     * @throws Exception
     */
    @Test
    @DisplayName("?????? ????????????")
    void JOIN() throws Exception {
        //given
        List<Member> teamA = queryFactory.selectFrom(member)
                .join(member.team, team)  //????????? Qteam.team
                .where(team.name.eq("teamA"))
                .fetch();
        //when

        //then

    }


    /**
     * ????????? ?????? ?????? ?????? ??????
     * -> ???????????? ?????? ?????? ??????
     * @throws Exception
     */
    @Test
    @DisplayName("??????")
    void subQuery() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = queryFactory.selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions.select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(fetch).extracting("age").containsExactly(30,40);
    }

    @Test
    @DisplayName("in ?????? ????????? ?????? * ??????*")
    void subQuer2y() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = queryFactory.selectFrom(member)
                .where(member.age.in(
                        JPAExpressions.
                                select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(fetch).extracting("age").containsExactly(30,40,20);
    }

    @Test
    @DisplayName("select ???????????? ?????? ?????? ??????.")
    void selectsubquery() throws Exception {
        QMember memberSub = new QMember("memberSub");
        //given
        queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        //when

        //then
    }

    @Test
    @DisplayName("")
    void basicCase() throws Exception {
        //given
        List<String> fetch = queryFactory.select(member.age
                        .when(10).then("??????")
                        .when(20).then("?????????")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("??????")
    void simpleProjection() throws Exception {
        //given
        List<String> fetch = queryFactory.select(member.username)
                .from(member)
                .fetch();
        //when

    }

    @Test
    @DisplayName("?????????. member??? ????????? age??? ???????????????.")
    void tupleProjection() throws Exception {
        //given
        List<Tuple> fetch = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        //when
        for (Tuple tuple : fetch) {
            String s = tuple.get(member.username);
            Integer integer = tuple.get(member.age);


        }

        //then

    }

    @Test
    @DisplayName("dto??? ??????1")
    void findDtdoBySetter() throws Exception {
        //given

        List<MemberDto> fetch = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username
                        , member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("dto??? ??????2")
    void findDtdoBySetter_2() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username
                        , member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtdoByQueryProjection() throws Exception {
        //given
        List<MemberDto> fetch = queryFactory.select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    @DisplayName("")
    void dynamicQuery_BooleanBuilder() throws Exception {
        //given
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond !=null){
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond!=null)
            builder.and(member.age.eq(ageCond));

        return    queryFactory.selectFrom(member)
                .where(builder)
                .fetch();
    }
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond),ageEq(ageCond))
                .fetch();
    }
    private Predicate usernameEq(String usernameCond) {
        return usernameCond == null ? null : member.username.eq(usernameCond);
    }
    private Predicate ageEq(Integer ageCond) {
        return ageCond == null ? null : member.age.eq(ageCond);
    }

    @Test
    @DisplayName("")
    @Commit
    void Bulkupdate() throws Exception {
        //given
        long count = queryFactory
                .update(member)
//                .set(member.username, "?????????")
                .set(member.age, member.age.add(1))
                .where(member.age.lt(28))

                .execute();
    }

    @Test
    @DisplayName("replace????????????")
    void replaceuse() throws Exception {
        //given
        List<String> fetch = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace',{0},{1},{2})",
                        member.username, "member", "M"
                ))
                .from(member)
                .fetch();
        //when
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("")
    void good() throws Exception {
        //given
        //when

        //then

    }


}
