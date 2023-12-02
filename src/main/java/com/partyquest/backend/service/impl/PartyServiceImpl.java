package com.partyquest.backend.service.impl;

import com.partyquest.backend.config.exception.*;
import com.partyquest.backend.domain.dto.PartyDto;
import com.partyquest.backend.domain.dto.RepositoryDto;
import com.partyquest.backend.domain.entity.File;
import com.partyquest.backend.domain.entity.Party;
import com.partyquest.backend.domain.entity.User;
import com.partyquest.backend.domain.entity.UserParty;
import com.partyquest.backend.domain.repository.FileRepository;
import com.partyquest.backend.domain.repository.PartyRepository;
import com.partyquest.backend.domain.repository.UserPartyRepository;
import com.partyquest.backend.domain.repository.UserRepository;
import com.partyquest.backend.domain.type.FileType;
import com.partyquest.backend.domain.type.PartyMemberType;
import com.partyquest.backend.service.logic.PartyService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.partyquest.backend.domain.dto.PartyDto.*;

@Service
@Slf4j
public class PartyServiceImpl implements PartyService {

    private final PartyRepository partyRepository;
    private final UserRepository userRepository;
    private final UserPartyRepository userPartyRepository;
    private final FileRepository fileRepository;

    @Autowired
    public PartyServiceImpl(PartyRepository partyRepository,
                            UserRepository userRepository,
                            UserPartyRepository userPartyRepository,
                            FileRepository fileRepository) {
        this.partyRepository = partyRepository;
        this.userRepository = userRepository;
        this.userPartyRepository = userPartyRepository;
        this.fileRepository = fileRepository;
    }

    private User getUserData(long makerId) {
        Optional<User> optional = userRepository.findById(makerId);
        if(optional.isPresent()) return optional.get();
        else throw new EmailNotFoundException("USER NOT FOUND", ErrorCode.INTER_SERVER_ERROR);
    }

    @Override
    @Transactional
    public PartyDto.CreatePartyDto.Response createParty(PartyDto.CreatePartyDto.Request request, long makerID) {
        User user = getUserData(makerID);
        Party party = partyRepository.save(CreatePartyDto.Request.dtoToEntity(request));

        UserParty userParty = UserParty.builder()
                .user(user)
                .registered(true)
                .memberGrade(PartyMemberType.MASTER)
                .party(party)
                .partyAdmin(true)
                .build();

        userPartyRepository.save(userParty);

        party.getUserParties().add(userParty);
        user.getUserParties().add(userParty);

        File file = File.builder()
                .party(party)
                .fileAttachChngName("test code")
                .filePath("test path")
                .fileOriginalName("test origin")
                .type(FileType.PARTY_THUMBNAIL)
                .fileSize(1234)
                .build();

        fileRepository.save(file);
        party.getFiles().add(file);

        return PartyDto.CreatePartyDto.Response.entityToDto(party);
    }

    //검색 키워드, 파티이름, 파티장
    @Override
    public List<PartyDto.ReadPartyDto.Response> readPartyDto(String master, String title, Long id) {
        List<RepositoryDto.ReadPartiesVO> voList = partyRepository.getPartiesTmp(master, title, id);
        List<ReadPartyDto.Response> result = new ArrayList<>();
        for(RepositoryDto.ReadPartiesVO vo : voList) {
            result.add(
                    ReadPartyDto.Response.builder()
                            .title(vo.getPartyTitle())
                            .partyMaster(vo.getPartyMaster())
                            .capability((int)vo.getPartyMemberCnt())
                            .thumbnailPath(vo.getPartyThumbnailPath())
                            .id(vo.getPartyId())
                            .build()
            );
        }
        return result;
    }

    @Override
    public PartyDto.ReadPartyDto.Response readPartyDto(long id) {
        return null;
    }

    @Override
    public ApplicationPartyDto.Response ApplicationParty(ApplicationPartyDto.Request requestDto, long userID) {
        Optional<User> optUser = userRepository.findById(userID);
        Optional<Party> optParty = partyRepository.getParty(requestDto.getPartyId());
        if(optUser.isEmpty()) throw new EmailNotFoundException("NOT FOUND USER",ErrorCode.EMAIL_NOT_FOUND);
        if(optParty.isEmpty()) throw new PartyNotFoundException("NOT FOUND PARTY",ErrorCode.PARTY_NOT_FOUND);

        User user = optUser.get();
        Party party = optParty.get();

        Optional<UserParty> optUserParty = userPartyRepository.existEntryUser(party,user);
        UserParty userParty;
        if(optUserParty.isPresent()) {
            userParty = optUserParty.get();
            log.info(userParty.getMemberGrade().toString());
            if(userParty.getIsDelete()) {
                userParty.setIsDelete(false);
                userPartyRepository.save(userParty);
            } else {
                throw new PartyApplicationDuplicateException(ErrorCode.PARTY_APPLICATION_DUPLICATED,"ALREADY APPLICATED USER");
            }
        } else {
            userParty = UserParty.builder()
                    .partyAdmin(false)
                    .party(party)
                    .user(user)
                    .registered(false)
                    .memberGrade(PartyMemberType.NO_MEMBER)
                    .build();
            userParty = userPartyRepository.save(userParty);

            user.getUserParties().add(userParty);
            party.getUserParties().add(userParty);
        }

        ApplicationPartyDto.Response response = ApplicationPartyDto.Response.builder()
                .partyId(party.getId())
                .userPartyId(userParty.getId())
                .userId(userParty.getId())
                .build();

        return response;
    }

    @Override
    public boolean deleteParty(List<Long> partyIds) {
        try {
            for (Long partyId : partyIds) {
                Optional<Party> party = partyRepository.findById(partyId);
                if (party.isPresent()) {
                    Party tmp = party.get();
                    tmp.setIsDelete(true);
                    partyRepository.save(tmp);
                }

            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<ReadPartyMemberDto.Response> getMemberFromGrade(Long partyId, PartyMemberType grade) {
        if(partyRepository.findById(partyId).isEmpty()) throw new PartyNotFoundException("NOT FOUND PARTY",ErrorCode.PARTY_NOT_FOUND);

        List<RepositoryDto.PartyMemberVO> memberFromGrade = userPartyRepository.findMemberFromGrade(partyId, grade);
        return memberFromGrade.stream()
                .map(member -> ReadPartyMemberDto.Response.builder()
                        .grade(member.getGrade())
                        .partyID(partyId)
                        .filePath(member.getFilePath())
                        .nickname(member.getNickname())
                        .registered(member.isRegistered())
                        .userID(member.getUserID())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPartyDto.Response> getMembershipParties(long userId) {
        Optional<User> optUser = userRepository.findById(userId);
        if(optUser.isEmpty()) throw new EmailNotFoundException("NOT FOUND USER",ErrorCode.EMAIL_NOT_FOUND);
        List<RepositoryDto.MembershipDto> membershipDtoList = userPartyRepository.findMembershipUser(optUser.get());

        List<MembershipPartyDto.Response> result = new ArrayList<>();
        for(RepositoryDto.MembershipDto dto : membershipDtoList) {
            result.add(MembershipPartyDto.Response.builder()
                            .partyID(dto.getId())
                            .memberGrade(dto.getGrade().toString())
                    .partyMaster(dto.getPartyMaster())
                    .partyMemberCnt(dto.getPartyMemberCnt())
                    .partyThumbnailPath(dto.getPartyThumbnailPath())
                    .partyTitle(dto.getPartyTitle())
                    .build());
        }
        return result;
    }

    @Override
    public ReadPartyDto.Response readPartySpecification(Long id) {
        RepositoryDto.ReadPartyVO vo = partyRepository.getPartiesTmp(id);
        return ReadPartyDto.Response.builder()
                .thumbnailPath(vo.getPartyThumbnailPath())
                .description(vo.getDescription())
                .capability((int)vo.getPartyMemberCnt())
                .partyMaster(vo.getPartyMaster())
                .id(vo.getPartyId())
                .title(vo.getPartyTitle())
                .build();
    }

    @Override
    public void AcceptPartyApplicator(ApplicationPartyDto.AcceptRequest dto, long masterID) {
        //입력 값 - 유저 데이터 검증
        if(!userRepository.isUser(dto.getUserID()))
            throw new EmailNotFoundException("NOT FOUND USER",ErrorCode.EMAIL_NOT_FOUND);

        //유저 데이터가 정확히 해당 파티를 신청했는지 검증 -> userParty 테이블에서 dto.getPartyID로 파티 일치하는지 조회
        if(!userPartyRepository.isApplicationUser(dto.getUserID(),dto.getPartyID()))
            throw new NotPartyMemberException("NOT PARTY MEMBER",ErrorCode.NOT_PARTY_MEMBER);

        //파티 마스터가 정확히 권한을 가지고 있는 유저인가?
        if(!userPartyRepository.isMasterAndAdminUserTmp(masterID, dto.getPartyID()))
            throw new NotAdminException("NOT ADMIN USER",ErrorCode.ACCESS_DENIED);


        userPartyRepository.updateAcceptApplicator(dto.getUserID());
    }
}
