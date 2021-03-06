package org.iron.ultimate.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.iron.ultimate.exception.MethodFailureException;
import org.iron.ultimate.exception.UserNotFoundException;
import org.iron.ultimate.model.AccountTypeDTO;
import org.iron.ultimate.model.HiscoreEntry;
import org.iron.ultimate.model.MultiHiscoreEntry;
import org.iron.ultimate.model.PrettyMultiHiscoreEntry;
import org.iron.ultimate.model.PrettyUserProfileDTO;
import org.iron.ultimate.model.SkillDTO;
import org.iron.ultimate.model.UserProfileDTO;
import org.iron.ultimate.model.enums.AccountType;
import org.iron.ultimate.model.enums.HiscoreCategory;
import org.iron.ultimate.model.enums.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class HiscoreService {
	
	@Autowired
	private MetaDataService metaDataService;
	
	private static final String hiscoreLiteUrlStart = "https://services.runescape.com/m=";
	private static final String hiscoreLiteUrlMid = "/index_lite.ws?player=";

	public String getRawHiscores(HiscoreCategory category, String username) throws UserNotFoundException {
		String hiscoreLiteUrl = hiscoreLiteUrlStart + category + hiscoreLiteUrlMid + username;
		RestTemplate restTemplate = new RestTemplate();
		try {
			return restTemplate.getForObject(hiscoreLiteUrl, String.class);
		} catch ( HttpClientErrorException e ) {
			if ( e.getStatusCode() == HttpStatus.NOT_FOUND ) {
				throw new UserNotFoundException();
			} else {
				throw new MethodFailureException("Unexpected response recieved from Runescape hiscores");
			}
		}
	}

	public List<HiscoreEntry> getHiscores(HiscoreCategory category, String username) throws UserNotFoundException {
		List<HiscoreEntry> userHiscoreEntries = new ArrayList<HiscoreEntry>();
		List<SkillDTO> hiscoreSkills = metaDataService.getListOfSkills();
		
		String rawHiscoresLiteData = getRawHiscores(category, username);
		if ( rawHiscoresLiteData != null && !rawHiscoresLiteData.isEmpty() ) {
			rawHiscoresLiteData = rawHiscoresLiteData.replaceAll("\r\n", " ");
			rawHiscoresLiteData = rawHiscoresLiteData.replaceAll("\n", " ");
			String[] liteHiscoreTuples = rawHiscoresLiteData.split(" ");
			if ( liteHiscoreTuples != null ) {
				for ( int i=0; i<liteHiscoreTuples.length; i++ ) {
					
					Long rank = null;
					Long level = -1L;
					Long experience = null;
					
					String liteHiscoreTuple = liteHiscoreTuples[i];
					if ( liteHiscoreTuple != null && !liteHiscoreTuple.isEmpty() ) {
						String[] liteHiscoreTupleElements = liteHiscoreTuple.split(",");
						if ( liteHiscoreTupleElements != null ) {
							if ( liteHiscoreTupleElements.length == 3 ) { // Regular skill
								rank = Long.parseLong(liteHiscoreTupleElements[0]);
								level = Long.parseLong(liteHiscoreTupleElements[1]);
								experience = Long.parseLong(liteHiscoreTupleElements[2]);
							} else if ( liteHiscoreTupleElements.length == 2 ) { // Other
								rank = Long.parseLong(liteHiscoreTupleElements[0]);
								experience = Long.parseLong(liteHiscoreTupleElements[1]);
							}
						}
					}
					
					if ( experience != null && i < hiscoreSkills.size() ) {
						SkillDTO skillDto = hiscoreSkills.get(i);
						userHiscoreEntries.add(new HiscoreEntry(skillDto, rank, level, experience));
					}
				}
			}
		}
		return userHiscoreEntries;
	}
	
	public Map<String, Map<String, Long>> getPrettyHiscores(HiscoreCategory category, String username) throws UserNotFoundException {
		Map<String, Map<String, Long>> prettyUserHiscores = new LinkedHashMap<String, Map<String, Long>>();
		List<HiscoreEntry> userHiscores = getHiscores(category, username);
		for ( HiscoreEntry userHiscore : userHiscores ) {
			String displayName = userHiscore.getDisplayName();
			if ( displayName != null && !displayName.isEmpty() ) {
				Map<String, Long> attributeValueMap = new LinkedHashMap<String, Long>();
				Long rank = userHiscore.getRank();
				Long level = userHiscore.getLevel();
				Long experience = userHiscore.getExperience();
				if ( rank != null ) {
					attributeValueMap.put("Rank", rank);
				}
				if ( level != null ) {
					attributeValueMap.put("Level", level);
				}
				if ( experience != null ) {
					attributeValueMap.put("Experience", experience);
				}
				prettyUserHiscores.put(displayName, attributeValueMap);
			}
		}
		return prettyUserHiscores;
	}

	public UserProfileDTO getUserProfile(String username) throws UserNotFoundException {
		
		UserProfileDTO userProfileDto = new UserProfileDTO();
		userProfileDto.setUsername(username);
		
		List<HiscoreEntry> regularHiscores = null;
		List<HiscoreEntry> ironmanHiscores = null;
		List<HiscoreEntry> hardcoreHiscores = null;
		List<HiscoreEntry> ultimateHiscores = null;
		try {
			regularHiscores = getHiscores(HiscoreCategory.REGULAR, username);
			ironmanHiscores = getHiscores(HiscoreCategory.IRONMAN, username);
			try {
				hardcoreHiscores = getHiscores(HiscoreCategory.HARDCORE, username);
			} catch ( UserNotFoundException e ) {}
			if ( hardcoreHiscores == null ) {
				ultimateHiscores = getHiscores(HiscoreCategory.ULTIMATE, username);
			}
		} catch (UserNotFoundException e) {
			if ( regularHiscores == null ) {
				throw e;
			}
		}
		
		boolean isUltimate = isEqualOrMoreMetal(ultimateHiscores, regularHiscores);
		boolean isHardcore = isEqualOrMoreMetal(hardcoreHiscores, regularHiscores);
		boolean isIronman = isEqualOrMoreMetal(ironmanHiscores, regularHiscores);
		
		boolean wasUltimate = ultimateHiscores != null;
		boolean wasHardcore = hardcoreHiscores != null;
		boolean wasIronman = ironmanHiscores != null && !isUltimate && !isHardcore;
				
		Map<String, AccountTypeDTO> accountTypeMap = metaDataService.getAccountTypeMap();
		AccountType currentType = isUltimate ? AccountType.ULTIMATE : (isHardcore ? AccountType.HARDCORE : (isIronman ? AccountType.IRONMAN : AccountType.REGULAR));
		userProfileDto.setCurrentAccountType(accountTypeMap.get(currentType.name()));
		
		List<AccountTypeDTO> pastAccountTypes = new ArrayList<AccountTypeDTO>();
		if ( wasUltimate && currentType != AccountType.ULTIMATE ) {
			pastAccountTypes.add(accountTypeMap.get(AccountType.ULTIMATE.name()));
		}
		if ( wasHardcore && currentType != AccountType.HARDCORE ) {
			pastAccountTypes.add(accountTypeMap.get(AccountType.HARDCORE.name()));
		}
		if ( wasIronman && currentType != AccountType.IRONMAN ) {
			pastAccountTypes.add(accountTypeMap.get(AccountType.IRONMAN.name()));
		}
		userProfileDto.setPastAccountTypes(pastAccountTypes);
		
		List<AccountTypeDTO> relevantAccountTypes = new ArrayList<AccountTypeDTO>();
		Map<String, List<HiscoreEntry>> accountTypeHiscores = new LinkedHashMap<String, List<HiscoreEntry>>();
		relevantAccountTypes.add(accountTypeMap.get(AccountType.REGULAR.name()));
		accountTypeHiscores.put(AccountType.REGULAR.name(), regularHiscores);
		if ( ironmanHiscores != null ) {
			relevantAccountTypes.add(accountTypeMap.get(AccountType.IRONMAN.name()));
			accountTypeHiscores.put(AccountType.IRONMAN.name(), ironmanHiscores);
		}
		if ( hardcoreHiscores != null ) {
			relevantAccountTypes.add(accountTypeMap.get(AccountType.HARDCORE.name()));
			accountTypeHiscores.put(AccountType.HARDCORE.name(), hardcoreHiscores);
		}
		if ( ultimateHiscores != null ) {
			relevantAccountTypes.add(accountTypeMap.get(AccountType.ULTIMATE.name()));
			accountTypeHiscores.put(AccountType.ULTIMATE.name(), ultimateHiscores);
		}
		userProfileDto.setRelevantAccountTypes(relevantAccountTypes);
		
		Map<String, MultiHiscoreEntry> multiHiscores = getMultiHiscore(accountTypeHiscores, pastAccountTypes);
		userProfileDto.setHiscores(multiHiscores);
		
		Set<String> skillNames = multiHiscores.keySet();
		for ( String skillName : skillNames ) {
			MultiHiscoreEntry multiHiscore = multiHiscores.get(skillName);
			if ( skillName != null && skillName.equalsIgnoreCase(Skill.OVERALL.name()) ) {
				userProfileDto.setCurrentTotalLevel(multiHiscore.getLevel());
				userProfileDto.setCurrentTotalExperience(multiHiscore.getExperience());
				userProfileDto.setPastAccountTypeTotalLevels(multiHiscore.getPastAccountTypeLevels());
				userProfileDto.setPastAccountTypeTotalExperience(multiHiscore.getPastAccountTypeExperience());
				userProfileDto.setAccountTypeRanks(multiHiscore.getAccountTypeRanks());
			}
		}
		
		return userProfileDto;
	}

	public PrettyUserProfileDTO getPrettyUserProfile(String username) throws UserNotFoundException {
		
		UserProfileDTO userProfile = getUserProfile(username);
		
		Map<String, AccountTypeDTO> accountTypeMap = metaDataService.getAccountTypeMap();
		
		PrettyUserProfileDTO prettyUserProfile = new PrettyUserProfileDTO();
		
		prettyUserProfile.setUsername(userProfile.getUsername());
		prettyUserProfile.setAccountType(userProfile.getCurrentAccountType().getDisplayName());
		prettyUserProfile.setTotalLevel(userProfile.getCurrentTotalLevel());
		prettyUserProfile.setTotalExperience(userProfile.getCurrentTotalExperience());
		
		Map<String, Long> accountTypeRanks = userProfile.getAccountTypeRanks();
		Set<String> rankedAccountTypes = accountTypeRanks.keySet();
		Map<String, Long> prettyAccountTypeRanks = new LinkedHashMap<String, Long>();
		for ( String rankedAccountType : rankedAccountTypes ) {
			String displayName = accountTypeMap.get(rankedAccountType).getDisplayName();
			Long rank = accountTypeRanks.get(rankedAccountType);
			prettyAccountTypeRanks.put(displayName, rank);
		}
		prettyUserProfile.setAccountTypeRanks(prettyAccountTypeRanks);
		
		List<AccountTypeDTO> pastAccountTypeDtos = userProfile.getPastAccountTypes();
		Set<String> pastAccountTypes = new LinkedHashSet<String>();
		for ( AccountTypeDTO pastAccountTypeDto : pastAccountTypeDtos ) {
			pastAccountTypes.add(pastAccountTypeDto.getAccountType());
		}
		List<String> prettyPastAccountTypes = new ArrayList<String>();
		
		Map<String, Long> pastAccountTypeTotalLevels = userProfile.getPastAccountTypeTotalLevels();
		Map<String, Long> pastAccountTypeTotalExperience = userProfile.getPastAccountTypeTotalExperience();
		Map<String, Long> prettyPastAccountTypeTotalLevels = new LinkedHashMap<String, Long>();
		Map<String, Long> prettyPastAccountTypeTotalExperience = new LinkedHashMap<String, Long>();
		
		for ( String pastAccountType : pastAccountTypes ) {
			String displayName = accountTypeMap.get(pastAccountType).getDisplayName();
			prettyPastAccountTypes.add(displayName);
			
			Long total = pastAccountTypeTotalLevels.get(pastAccountType);
			prettyPastAccountTypeTotalLevels.put(displayName, total);
			
			Long totalExperience = pastAccountTypeTotalExperience.get(pastAccountType);
			prettyPastAccountTypeTotalExperience.put(displayName, totalExperience);
		}

		prettyUserProfile.setPastAccountTypes(prettyPastAccountTypes);
		prettyUserProfile.setPastAccountTypeTotalLevels(prettyPastAccountTypeTotalLevels);
		prettyUserProfile.setPastAccountTypeTotalExperience(prettyPastAccountTypeTotalExperience);
		
		Map<String, MultiHiscoreEntry> hiscores = userProfile.getHiscores();
		Set<String> skillNames = hiscores.keySet();
		Map<String, PrettyMultiHiscoreEntry> prettyMultiHiscoreEntries = new LinkedHashMap<String, PrettyMultiHiscoreEntry>();
		for ( String skillName : skillNames ) {
			MultiHiscoreEntry hiscore = hiscores.get(skillName);
			PrettyMultiHiscoreEntry prettyHiscore = new PrettyMultiHiscoreEntry();
			prettyMultiHiscoreEntries.put(hiscore.getDisplayName(), prettyHiscore);
			prettyHiscore.setLevel(hiscore.getLevel());
			prettyHiscore.setExperience(hiscore.getExperience());

			Map<String, Long> entryAccountTypeRanks = hiscore.getAccountTypeRanks();
			Map<String, Long> prettyEntryAccountTypeRanks = new LinkedHashMap<String, Long>();
			Set<String> accountTypesWithRank = entryAccountTypeRanks.keySet();
			for ( String accountTypeWithRank : accountTypesWithRank ) {
				String displayName = accountTypeMap.get(accountTypeWithRank).getDisplayName();
				Long rank = entryAccountTypeRanks.get(accountTypeWithRank);
				prettyEntryAccountTypeRanks.put(displayName, rank);
			}
			prettyHiscore.setAccountTypeRanks(prettyEntryAccountTypeRanks);
			
			Map<String, Long> pastAccountTypeLevels = hiscore.getPastAccountTypeLevels();
			Map<String, Long> pastAccountTypeExperience = hiscore.getPastAccountTypeExperience();
			Map<String, Long> prettyPastAccountTypeLevels = new LinkedHashMap<String, Long>();
			Map<String, Long> prettyPastAccountTypeExperience = new LinkedHashMap<String, Long>();
			for ( String pastAccountType : pastAccountTypes ) {
				String displayName = accountTypeMap.get(pastAccountType).getDisplayName();
				Long level = pastAccountTypeLevels.get(pastAccountType);
				if ( level != null && level >= 0 ) {
					prettyPastAccountTypeLevels.put(displayName, level);
				}
				Long experience = pastAccountTypeExperience.get(pastAccountType);
				if ( experience != null && experience >= 0 ) {
					prettyPastAccountTypeExperience.put(displayName, experience);
				}
			}
			prettyHiscore.setPastAccountTypeLevels(prettyPastAccountTypeLevels);
			prettyHiscore.setPastAccountTypeExperience(prettyPastAccountTypeExperience);
		}
		prettyUserProfile.setHiscores(prettyMultiHiscoreEntries);
		
		return prettyUserProfile;
	}

	private Map<String, MultiHiscoreEntry> getMultiHiscore(Map<String, List<HiscoreEntry>> accountTypeHiscores, List<AccountTypeDTO> pastAccountTypeDtos) {
		
		Set<String> pastAccountTypes = new LinkedHashSet<String>();
		for ( AccountTypeDTO pastAccountTypeDto : pastAccountTypeDtos ) {
			pastAccountTypes.add(pastAccountTypeDto.getAccountType());
		}
		
		Set<String> accountTypes = accountTypeHiscores.keySet();
		List<HiscoreEntry> regularHiscores = accountTypeHiscores.get(AccountType.REGULAR.name());
		if ( regularHiscores == null ) {
			throw new MethodFailureException("Cannot get multi highscores without regular hiscores!");
		} else { // Regular highscores not null
			int totalRegularEntries = regularHiscores.size();
			for ( String accountType : accountTypes ) {
				if ( totalRegularEntries != accountTypeHiscores.get(accountType).size() ) {
					throw new MethodFailureException("Cannot get multi highscores with mismatched list sizes!");
				}
			}
		}
		
		Map<String, MultiHiscoreEntry> multiHiscores = new LinkedHashMap<String, MultiHiscoreEntry>();
		int totalEntries = regularHiscores.size();
		for ( int i=0; i<totalEntries; i++ ) {
			MultiHiscoreEntry multiHiscoreEntry = new MultiHiscoreEntry();
			HiscoreEntry regularEntry = regularHiscores.get(i);
			String skillName = regularEntry.getSkillName();
			
			multiHiscoreEntry.setSkillId(regularEntry.getSkillId());
			multiHiscoreEntry.setSkillName(skillName);
			multiHiscoreEntry.setDisplayName(regularEntry.getDisplayName());
			multiHiscoreEntry.setAbbreviation(regularEntry.getAbbreviation());
			multiHiscoreEntry.setLiteIndex(regularEntry.getLiteIndex());
			
			Set<Long> levels = new LinkedHashSet<Long>();
			Set<Long> experiences = new LinkedHashSet<Long>();
			Map<String, Long> accountTypeRanks = new LinkedHashMap<String, Long>();
			Map<String, Long> pastAccountTypeLevels = new LinkedHashMap<String, Long>();
			Map<String, Long> pastAccountTypeExperiences = new LinkedHashMap<String, Long>();
			
			for ( String accountType : accountTypes ) {
				List<HiscoreEntry> hiscoreEntries = accountTypeHiscores.get(accountType);
				HiscoreEntry hiscoreEntry = hiscoreEntries.get(i);
				Long rank = hiscoreEntry.getRank();
				Long level = hiscoreEntry.getLevel();
				Long experience = hiscoreEntry.getExperience();
				boolean isPastAccountType = pastAccountTypes.contains(accountType);
				if ( rank != null && rank >= 0 ) {
					accountTypeRanks.put(accountType, rank);
				}
				if ( level != null && level >= 0 ) {
					levels.add(level);
					if ( isPastAccountType ) {
						pastAccountTypeLevels.put(accountType, level);
					}
				}
				if ( experience != null && experience >= 0 ) {
					experiences.add(experience);
					if ( isPastAccountType ) {
						pastAccountTypeExperiences.put(accountType, experience);
					}
				}
			}
			
			if ( !levels.isEmpty() ) {
				multiHiscoreEntry.setLevel(Collections.max(levels));
			} else {
				multiHiscoreEntry.setLevel(-1L);
			}
			if ( !experiences.isEmpty() ) {
				multiHiscoreEntry.setExperience(Collections.max(experiences));
			} else {
				multiHiscoreEntry.setExperience(-1L);
			}
			multiHiscoreEntry.setAccountTypeRanks(accountTypeRanks);
			
			multiHiscoreEntry.setPastAccountTypeLevels(pastAccountTypeLevels);
			multiHiscoreEntry.setPastAccountTypeExperience(pastAccountTypeExperiences);
			
			multiHiscores.put(skillName, multiHiscoreEntry);
		}
		return multiHiscores;
	}
	
	private boolean isEqualOrMoreMetal(List<HiscoreEntry> moreMetalType, List<HiscoreEntry> lessMetalType) {
		if ( moreMetalType == null && lessMetalType == null ) {
			return true;
		} else if ( moreMetalType == null || lessMetalType == null ) {
			return false;
		} else {
			int numHiscoreEntries = moreMetalType.size();
			if ( numHiscoreEntries != lessMetalType.size() ) {
				return false;
			} else {
				for ( int i=0; i<numHiscoreEntries; i++ ) {
					HiscoreEntry moreMetalTypeEntry = moreMetalType.get(i);
					HiscoreEntry lessMetalTypeEntry = lessMetalType.get(i);
					if ( moreMetalTypeEntry == null && lessMetalTypeEntry != null ) {
						return false;
					} else if ( moreMetalTypeEntry == null || lessMetalTypeEntry == null ) {
						return false;
					} else {
						Long moreMetalExperience = moreMetalTypeEntry.getExperience();
						Long lessMetalExperience = lessMetalTypeEntry.getExperience();
						if ( moreMetalExperience != null && lessMetalExperience != null ) {
							if ( moreMetalExperience < lessMetalExperience ) {
								return false;
							}
						} else if ( moreMetalExperience == null || lessMetalExperience == null ) {
							return false;
						}
					}
				}
			}
			return true;
		}
	}
	
}
