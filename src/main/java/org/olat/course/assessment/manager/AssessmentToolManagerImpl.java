/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.course.assessment.manager;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.TypedQuery;

import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.IdentityImpl;
import org.olat.basesecurity.IdentityShort;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.id.Identity;
import org.olat.core.util.StringHelper;
import org.olat.course.assessment.AssessmentToolManager;
import org.olat.course.assessment.model.AssessedBusinessGroup;
import org.olat.course.assessment.model.AssessmentStatistics;
import org.olat.course.assessment.model.SearchAssessedIdentityParams;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.model.AssessmentEntryStatus;
import org.olat.repository.RepositoryEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Work with the datas for the assessment tool
 * 
 * 
 * Initial date: 21.07.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class AssessmentToolManagerImpl implements AssessmentToolManager {

	@Autowired
	private DB dbInstance;

	@Override
	public int getNumberOfAssessedIdentities(Identity coach, SearchAssessedIdentityParams params) {
		//count all possible participants for the coach permissions
		TypedQuery<Long> countUsers = createAssessedIdentities(coach, params, Long.class);
		int numOfAssessedIdentites = 0;
		List<Long> numOfUsersList = countUsers.getResultList();
		if(numOfUsersList.size() == 1) {
				numOfAssessedIdentites = numOfUsersList.get(0) == null ? 0 : numOfUsersList.get(0).intValue();
		}
		return numOfAssessedIdentites;
	}


	@Override
	public int getNumberOfParticipants(Identity coach, SearchAssessedIdentityParams params) {
		RepositoryEntry courseEntry = params.getEntry();
		
		int numOfParticipants = 0;
		if(params.isAdmin()) {
			StringBuilder sb = new StringBuilder();
			sb.append("select count(participant.identity.key) from repoentrytogroup as rel")
	          .append("  inner join rel.group as bGroup")
	          .append("  inner join bGroup.members as participant on (participant.role='").append(GroupRoles.participant.name()).append("')")
	          .append("  where rel.entry.key=:repoEntryKey)");
			
			List<Number> count = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("repoEntryKey", courseEntry.getKey())
				.getResultList();
			numOfParticipants = count == null || count.isEmpty() || count.get(0) == null ? 0 : count.get(0).intValue();
			
			//count the users which login but are not members of the course
			if(params.isNonMembers()) {
				StringBuilder sc = new StringBuilder();
				sc.append("select count(infos.key) from usercourseinfos as infos ")
				  .append(" inner join infos.resource as infosResource on (infosResource.key=:resourceKey)")
				  .append(" where not exists (select membership.identity from repoentrytogroup as rel, bgroupmember as membership")
		          .append("   where rel.entry.key=:repoEntryKey and rel.group.key=membership.group.key and membership.identity.key=infos.identity.key")
		          .append("  )");
				
				List<Number> countAlt = dbInstance.getCurrentEntityManager()
					.createQuery(sc.toString(), Number.class)
					.setParameter("repoEntryKey", courseEntry.getKey())
					.setParameter("resourceKey", courseEntry.getOlatResource().getKey())
					.getResultList();
				numOfParticipants += countAlt == null || countAlt.isEmpty() || countAlt.get(0) == null ? 0 : countAlt.get(0).intValue();
			}
			
		} else if(params.isBusinessGroupCoach() || params.isRepositoryEntryCoach()) {
			StringBuilder sb = new StringBuilder();
			sb.append("select count(participant.identity.key) from repoentrytogroup as rel")
	          .append("  inner join rel.group as bGroup")
	          .append("  inner join bGroup.members as coach on (coach.identity.key=:identityKey and coach.role='").append(GroupRoles.coach.name()).append("')")
	          .append("  inner join bGroup.members as participant on (participant.role='").append(GroupRoles.participant.name()).append("')")
	          .append("  where rel.entry.key=:repoEntryKey)");
			
			List<Number> count = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("identityKey", coach.getKey())
				.setParameter("repoEntryKey", courseEntry.getKey())
				.getResultList();
			numOfParticipants = count == null || count.isEmpty() || count.get(0) == null ? 0 : count.get(0).intValue();
		}
		return numOfParticipants;
	}

	@Override
	public int getNumberOfInitialLaunches(Identity coach, SearchAssessedIdentityParams params) {
		RepositoryEntry courseEntry = params.getEntry();

		StringBuilder sf = new StringBuilder();
		sf.append("select count(infos.key), infosResource.key from usercourseinfos as infos ")
		  .append(" inner join infos.resource as infosResource on (infosResource.key=:resourceKey)")
		  .append(" where (infos.identity in");
		if(params.isAdmin()) {
			sf.append(" (select participant.identity from repoentrytogroup as rel, bgroupmember as participant")
	          .append("    where rel.entry.key=:repoEntryKey and rel.group=participant.group")
	          .append("      and participant.role='").append(GroupRoles.participant.name()).append("'")
	          .append("  )");
			if(params.isNonMembers()) {
				sf.append(" or not exists (select membership.identity from repoentrytogroup as rel, bgroupmember as membership")
		          .append("    where rel.entry.key=:repoEntryKey and rel.group.key=membership.group.key and membership.identity.key=infos.identity.key")
		          .append("  )");
			}
		} else if(params.isBusinessGroupCoach() || params.isRepositoryEntryCoach()) {
			sf.append(" (select participant.identity from repoentrytogroup as rel, bgroupmember as participant, bgroupmember as coach")
	          .append("    where rel.entry.key=:repoEntryKey")
	          .append("      and rel.group.key=coach.group.key and coach.role='").append(GroupRoles.coach.name()).append("' and coach.identity.key=:identityKey")
	          .append("      and rel.group.key=participant.group.key and participant.role='").append(GroupRoles.participant.name()).append("'")
	          .append("  )");
		}
		sf.append(" ) group by infosResource.key");

		TypedQuery<Object[]> infos = dbInstance.getCurrentEntityManager()
			.createQuery(sf.toString(), Object[].class)
			.setParameter("resourceKey", courseEntry.getOlatResource().getKey())
			.setParameter("repoEntryKey", courseEntry.getKey());
		if(!params.isAdmin()) {
			infos.setParameter("identityKey", coach.getKey());
		}

		List<Object[]> results = infos.getResultList();
		return results != null && results.size() > 0 && results.get(0)[0] instanceof Number
				? ((Number)results.get(0)[0]).intValue(): 0;
	}

	@Override
	public List<AssessedBusinessGroup> getBusinessGroupStatistics(Identity coach, SearchAssessedIdentityParams params) {
		RepositoryEntry courseEntry = params.getEntry();

		StringBuilder sf = new StringBuilder();
		sf.append("select bgi.key, bgi.name, baseGroup.key,")
		  .append(" avg(aentry.score) as scoreAverage,")
		  .append(" sum(case when aentry.passed=true then 1 else 0 end) as numOfPassed,")
		  .append(" sum(case when aentry.passed=false then 1 else 0 end) as numOfFailed,")
		  .append(" sum(case when (aentry.status is null or not(aentry.status='").append(AssessmentEntryStatus.notStarted.name()).append("') or aentry.passed is null) then 1 else 0 end) as numOfNotAttempted,")
		  .append(" (select count(gmember.key) from bgroupmember as gmember")
		  .append("   where gmember.group.key=baseGroup.key and gmember.role='").append(GroupRoles.participant.name()).append("'")
		  .append(" ) as numOfParticipants")
		  .append(" from businessgroup as bgi")
		  .append(" inner join bgi.baseGroup as baseGroup")
		  .append(" inner join repoentrytogroup as rel on (rel.group.key=bgi.baseGroup.key and rel.entry.key=:repoEntryKey)")
		  .append(" left join baseGroup.members as bmember on (bmember.role='").append(GroupRoles.participant.name()).append("')")
		  .append(" left join assessmententry as aentry on (bmember.identity.key=aentry.identity.key and rel.entry.key = aentry.repositoryEntry.key)");

		boolean where = false;
		if(!params.isAdmin()) {
			where = PersistenceHelper.appendAnd(sf, where);
			sf.append(" bgi.key in (:groupKeys)");
		}
		if(params.getSubIdent() != null) {
			where = PersistenceHelper.appendAnd(sf, where);
			sf.append(" aentry.subIdent=:subIdent");
		}
		if(params.getReferenceEntry() != null) {
			where = PersistenceHelper.appendAnd(sf, where);
			sf.append(" aentry.referenceEntry.key=:referenceKey");
		}
		sf.append(" group by bgi.key, baseGroup.key");

		TypedQuery<Object[]> stats = dbInstance.getCurrentEntityManager()
				.createQuery(sf.toString(), Object[].class)
				.setParameter("repoEntryKey", courseEntry.getKey());
		if(!params.isAdmin()) {
			stats.setParameter("groupKeys", params.getBusinessGroupKeys());
		}
		if(params.getSubIdent() != null) {
			stats.setParameter("subIdent", params.getSubIdent());
		}
		if(params.getReferenceEntry() != null) {
			stats.setParameter("referenceKey", params.getReferenceEntry().getKey());
		}
		
		List<Object[]> results = stats.getResultList();
		List<AssessedBusinessGroup> rows = new ArrayList<>(results.size());
		for(Object[] result:results) {
			Long key = (Long)result[0];
			String name = (String)result[1];
			double averageScore = result[3] == null ? 0.0d : ((Number)result[3]).doubleValue();
			int numOfPassed = result[4] == null ? 0 : ((Number)result[4]).intValue();
			int numOfFailed = result[5] == null ? 0  : ((Number)result[5]).intValue();
			int numOfNotAttempted = result[6] == null ? 0 : ((Number)result[6]).intValue();
			int numOfParticipants = result[7] == null ? 0 : ((Number)result[7]).intValue();

			rows.add(new AssessedBusinessGroup(key, name, averageScore,
					numOfPassed, numOfFailed, numOfNotAttempted,
					numOfParticipants));
		}
		return rows;
	}
	
	@Override
	public AssessmentStatistics getStatistics(Identity coach, SearchAssessedIdentityParams params) {
		RepositoryEntry courseEntry = params.getEntry();

		StringBuilder sf = new StringBuilder();
		sf.append("select avg(aentry.score) as scoreAverage, ")
		  .append(" sum(case when aentry.passed=true then 1 else 0 end) as numOfPassed,")
		  .append(" sum(case when aentry.passed=false then 1 else 0 end) as numOfFailed,")
		  .append(" sum(case when (aentry.status is null or not(aentry.status='").append(AssessmentEntryStatus.notStarted.name()).append("') or aentry.passed is null) then 1 else 0 end) as numOfNotAttempted,")
		  .append(" sum(aentry.key) as numOfStatements,")
		  .append(" v.key as repoKey")
		  .append(" from assessmententry aentry ")
		  .append(" inner join aentry.repositoryEntry v ")
		  .append(" where v.key=:repoEntryKey");
		if(params.getReferenceEntry() != null) {
			sf.append(" and aentry.referenceEntry.key=:referenceKey");
		}
		if(params.getSubIdent() != null) {
			sf.append(" and aentry.subIdent=:subIdent");
		}
		sf.append(" and (aentry.identity in");
		if(params.isAdmin()) {
			sf.append(" (select participant.identity from repoentrytogroup as rel, bgroupmember as participant")
	          .append("    where rel.entry.key=:repoEntryKey and rel.group=participant.group")
	          .append("      and participant.role='").append(GroupRoles.participant.name()).append("'")
	          .append("  )");
			if(params.isNonMembers()) {
				sf.append(" or aentry.identity not in (select membership.identity from repoentrytogroup as rel, bgroupmember as membership")
		          .append("    where rel.entry.key=:repoEntryKey and rel.group=membership.group and membership.identity=aentry.identity")
		          .append(" )");
			}
		} else if(params.isBusinessGroupCoach() || params.isRepositoryEntryCoach()) {
			sf.append(" (select participant.identity from repoentrytogroup as rel, bgroupmember as participant, bgroupmember as coach")
	          .append("    where rel.entry.key=:repoEntryKey")
	          .append("      and rel.group=coach.group and coach.role='").append(GroupRoles.coach.name()).append("' and coach.identity.key=:identityKey")
	          .append("      and rel.group=participant.group and participant.role='").append(GroupRoles.participant.name()).append("'")
	          .append("  )");
		}
		sf.append(" ) group by v.key");

		TypedQuery<Object[]> stats = dbInstance.getCurrentEntityManager()
			.createQuery(sf.toString(), Object[].class)
			.setParameter("repoEntryKey", courseEntry.getKey());
		if(!params.isAdmin()) {
			stats.setParameter("identityKey", coach.getKey());
		}
		if(params.getReferenceEntry() != null) {
			stats.setParameter("referenceKey", params.getReferenceEntry().getKey());
		}
		if(params.getSubIdent() != null) {
			stats.setParameter("subIdent", params.getSubIdent());
		}
		

		AssessmentStatistics entry = new AssessmentStatistics();
		List<Object[]> results = stats.getResultList();
		if(results != null && results.size() > 0) {
			Object[] result = results.get(0);
			Double averageScore = (Double)result[0];
			Long numOfPassed = (Long)result[1];
			Long numOfFailed = (Long)result[2];
			Long numOfNotAttempted = (Long)result[3];
			
			entry.setAverageScore(averageScore);
			entry.setCountPassed(numOfPassed == null ? 0 : numOfPassed.intValue());
			entry.setCountFailed(numOfFailed == null ? 0 : numOfFailed.intValue());
			entry.setCountNotAttempted(numOfNotAttempted == null ? 0 : numOfNotAttempted.intValue());
		}
		return entry;
	}

	@Override
	public List<Identity> getAssessedIdentities(Identity coach, SearchAssessedIdentityParams params) {
		TypedQuery<Identity> list = createAssessedIdentities(coach, params, Identity.class);
		return list.getResultList();
	}
	
	private <T> TypedQuery<T> createAssessedIdentities(Identity coach, SearchAssessedIdentityParams params, Class<T> classResult) {
		StringBuilder sb = new StringBuilder();
		sb.append("select ");
		if(Identity.class.equals(classResult)) {
			sb.append("ident").append(" from ").append(IdentityImpl.class.getName()).append(" as ident ")
			  .append(" inner join fetch ident.user user ");
		} else {
			sb.append("count(ident.key)").append(" from ").append(IdentityImpl.class.getName()).append(" as ident ")
			  .append(" inner join ident.user user ");
		}
		sb.append(" where ");
		if(params.getBusinessGroupKeys() != null && params.getBusinessGroupKeys().size() > 0) {
			sb.append(" ident.key in (select participant.identity.key from repoentrytogroup as rel, businessgroup bgi, bgroupmember as participant")
	          .append("    where rel.entry.key=:repoEntryKey and rel.group=bgi.baseGroup and rel.group=participant.group and bgi.key in (:businessGroupKeys) ")
	          .append("  )");
		} else if(params.isAdmin()) {
			sb.append(" (ident.key in (select participant.identity.key from repoentrytogroup as rel, bgroupmember as participant")
	          .append("    where rel.entry.key=:repoEntryKey and rel.group=participant.group")
	          .append("      and participant.role='").append(GroupRoles.participant.name()).append("'")
	          .append(" )");
			if(params.isNonMembers()) {
				sb.append(" or ident.key in (select aentry.identity.key from assessmententry aentry")
				  .append("  where aentry.repositoryEntry.key=:repoEntryKey")
				  .append("  and not exists (select membership.identity from repoentrytogroup as rel, bgroupmember as membership")
		          .append("    where rel.entry.key=:repoEntryKey and rel.group=membership.group and membership.identity=aentry.identity)")
		          .append(" )");
			}
			sb.append(")");
		} else if(params.isBusinessGroupCoach() || params.isRepositoryEntryCoach()) {
			sb.append(" ident.key in (select participant.identity.key from repoentrytogroup as rel, bgroupmember as participant, bgroupmember as coach")
	          .append("    where rel.entry.key=:repoEntryKey")
	          .append("      and rel.group=coach.group and coach.role='").append(GroupRoles.coach.name()).append("' and coach.identity.key=:identityKey")
	          .append("      and rel.group=participant.group and participant.role='").append(GroupRoles.participant.name()).append("'")
	          .append("  )");
		}
		
		Long identityKey = appendUserSearchByKey(sb, params.getSearchString());
		String[] searchArr = appendUserSearchFull(sb, params.getSearchString(), identityKey == null);

		TypedQuery<T> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), classResult)
				.setParameter("repoEntryKey", params.getEntry().getKey());
		if(!params.isAdmin()) {
			query.setParameter("identityKey", coach.getKey());
		}
		if(identityKey != null) {
			query.setParameter("searchIdentityKey", identityKey);
		}
		if(params.getBusinessGroupKeys() != null && params.getBusinessGroupKeys().size() > 0) {
			query.setParameter("businessGroupKeys", params.getBusinessGroupKeys());
		}
		appendUserSearchToQuery(searchArr, query);
		return query;
	}
	
	@Override
	public List<IdentityShort> getShortAssessedIdentities(Identity coach, SearchAssessedIdentityParams params, int maxResults) {
		StringBuilder sb = new StringBuilder();
		sb.append("select ident")
		  .append(" from bidentityshort as ident ")
		  .append(" where ");
		if(params.isAdmin()) {
			sb.append(" (ident.key in (select participant.identity.key from repoentrytogroup as rel, bgroupmember as participant")
	          .append("    where rel.entry.key=:repoEntryKey and rel.group=participant.group")
	          .append("      and participant.role='").append(GroupRoles.participant.name()).append("'")
	          .append(" )");
			if(params.isNonMembers()) {
				sb.append(" or ident.key in (select aentry.identity.key from assessmententry aentry")
				  .append("  where aentry.repositoryEntry.key=:repoEntryKey")
				  .append("  and not exists (select membership.identity from repoentrytogroup as rel, bgroupmember as membership")
		          .append("    where rel.entry.key=:repoEntryKey and rel.group=membership.group and membership.identity=aentry.identity)")
		          .append(" )");
			}
			sb.append(")");
		} else if(params.isBusinessGroupCoach() || params.isRepositoryEntryCoach()) {
			sb.append(" ident.key in (select participant.identity.key from repoentrytogroup as rel, bgroupmember as participant, bgroupmember as coach")
	          .append("    where rel.entry.key=:repoEntryKey")
	          .append("      and rel.group=coach.group and coach.role='").append(GroupRoles.coach.name()).append("' and coach.identity.key=:identityKey")
	          .append("      and rel.group=participant.group and participant.role='").append(GroupRoles.participant.name()).append("'")
	          .append("  )");
		}

		Long identityKey = appendUserSearchByKey(sb, params.getSearchString());
		String[] searchArr = appendUserSearch(sb, params.getSearchString());

		TypedQuery<IdentityShort> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), IdentityShort.class)
				.setFirstResult(0)
				.setMaxResults(maxResults)
				.setParameter("repoEntryKey", params.getEntry().getKey());
		if(!params.isAdmin()) {
			query.setParameter("identityKey", coach.getKey());
		}
		if(identityKey != null) {
			query.setParameter("searchIdentityKey", identityKey);
		}
		appendUserSearchToQuery(searchArr, query);
		return query.getResultList();
	}
	
	private Long appendUserSearchByKey(StringBuilder sb, String search) {
		Long identityKey = null;
		if(StringHelper.containsNonWhitespace(search)) {
			if(StringHelper.isLong(search)) {
				try {
					identityKey = new Long(search);
				} catch (NumberFormatException e) {
					//it can happens
				}
			}
			
			if(identityKey != null) {
				sb.append(" and ident.key=:searchIdentityKey");
			}
		}
		return identityKey;

	}
	
	private String[] appendUserSearch(StringBuilder sb, String search) {
		String[] searchArr = null;

		if(StringHelper.containsNonWhitespace(search)) {
			String dbVendor = dbInstance.getDbVendor();
			searchArr = search.split(" ");
			String[] attributes = new String[]{ "name", "firstName", "lastName", "email" };

			sb.append(" and (");
			boolean start = true;
			for(int i=0; i<searchArr.length; i++) {
				for(String attribute:attributes) {
					if(start) {
						start = false;
					} else {
						sb.append(" or ");
					}
					
					if (searchArr[i].contains("_") && dbVendor.equals("oracle")) {
						//oracle needs special ESCAPE sequence to search for escaped strings
						sb.append(" lower(ident.").append(attribute).append(") like :search").append(i).append(" ESCAPE '\\'");
					} else if (dbVendor.equals("mysql")) {
						sb.append(" ident.").append(attribute).append(" like :search").append(i);
					} else {
						sb.append(" lower(ident.").append(attribute).append(") like :search").append(i);
					}
				}
			}
			sb.append(")");
		}
		return searchArr;
	}
	
	private String[] appendUserSearchFull(StringBuilder sb, String search, boolean and) {
		String[] searchArr = null;

		if(StringHelper.containsNonWhitespace(search)) {
			String dbVendor = dbInstance.getDbVendor();
			searchArr = search.split(" ");
			String[] attributes = new String[]{ "firstName", "lastName", "email" };

			if(and) {
				sb.append(" and (");
			} else {
				sb.append(" or (");
			}
			boolean start = true;
			for(int i=0; i<searchArr.length; i++) {
				for(String attribute:attributes) {
					if(start) {
						start = false;
					} else {
						sb.append(" or ");
					}
					
					if(dbVendor.equals("mysql")) {
						sb.append(" user.").append(attribute).append(" like :search").append(i).append(" ");
					} else {
						sb.append(" lower(user.").append(attribute).append(") like :search").append(i).append(" ");
					}
					if(dbVendor.equals("oracle")) {
						sb.append(" escape '\\'");
					}
				}
			}
			sb.append(")");
		}
		return searchArr;
	}
	
	private void appendUserSearchToQuery(String[] searchArr, TypedQuery<?> query) {
		if(searchArr != null) {
			for(int i=searchArr.length; i-->0; ) {
				query.setParameter("search" + i, PersistenceHelper.makeFuzzyQueryString(searchArr[i]));
			}
		}
	}

	@Override
	public List<AssessmentEntry> getAssessmentEntries(Identity coach, SearchAssessedIdentityParams params, AssessmentEntryStatus status) {
		StringBuilder sb = new StringBuilder();
		sb.append("select aentry from assessmententry aentry")
		  .append(" inner join fetch aentry.identity as assessedIdentity")
		  .append(" inner join fetch assessedIdentity.user as assessedUser")
		  .append(" where aentry.repositoryEntry.key=:repoEntryKey");
		if(params.getReferenceEntry() != null) {
			sb.append(" and aentry.referenceEntry.key=:referenceKey");
		}
		if(params.getSubIdent() != null) {
			sb.append(" and aentry.subIdent=:subIdent");
		}
		if(status != null) {
			sb.append(" and aentry.status=:assessmentStatus");
		}
		sb.append(" and (assessedIdentity.key in");
		if(params.isAdmin()) {
			sb.append(" (select participant.identity.key from repoentrytogroup as rel, bgroupmember as participant")
	          .append("    where rel.entry.key=:repoEntryKey and rel.group=participant.group")
	          .append("      and participant.role='").append(GroupRoles.participant.name()).append("'")
	          .append("  )");
			if(params.isNonMembers()) {
				sb.append(" or assessedIdentity.key not in (select membership.identity.key from repoentrytogroup as rel, bgroupmember as membership")
		          .append("    where rel.entry.key=:repoEntryKey and rel.group=membership.group and membership.identity=aentry.identity")
		          .append(" )");
			}
		} else if(params.isBusinessGroupCoach() || params.isRepositoryEntryCoach()) {
			sb.append(" (select participant.identity.key from repoentrytogroup as rel, bgroupmember as participant, bgroupmember as coach")
	          .append("    where rel.entry.key=:repoEntryKey")
	          .append("      and rel.group=coach.group and coach.role='").append(GroupRoles.coach.name()).append("' and coach.identity.key=:identityKey")
	          .append("      and rel.group=participant.group and participant.role='").append(GroupRoles.participant.name()).append("'")
	          .append("  )");
		}
		sb.append(" )");
		
		TypedQuery<AssessmentEntry> list = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), AssessmentEntry.class)
			.setParameter("repoEntryKey", params.getEntry().getKey());
		if(params.getReferenceEntry() != null) {
			list.setParameter("referenceKey", params.getReferenceEntry().getKey());
		}
		if(params.getSubIdent() != null) {
			list.setParameter("subIdent", params.getSubIdent());
		}
		if(!params.isAdmin()) {
			list.setParameter("identityKey", coach.getKey());
		}
		if(status != null) {
			list.setParameter("assessmentStatus", status.name());
		}
		return list.getResultList();
	}
}
