package lt.jgreibus.magicdraw.redmine.plugin.actions;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.ui.dialogs.selection.ElementSelectionDlg;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.uml.ClassTypes;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DirectedRelationship;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.commonbehaviors.mdbasicbehaviors.Behavior;
import lt.jgreibus.magicdraw.redmine.tracker.manager.redmine.RedmineIssueManager;
import lt.jgreibus.magicdraw.redmine.utils.StereotypeUtils;

import java.awt.event.ActionEvent;
import java.util.*;

import static lt.jgreibus.magicdraw.redmine.element.manager.ElementSelectionManager.createStereotypedElementsSelectionDialog;

public class AddTestCasesToIssues extends MDAction{

    private final static String profileSysML = "SysML Profile";
    private final static String profileMed = "Softneta Medical Profile";

    public AddTestCasesToIssues(String id, String name) {
        super(id, name, null, null);
    }

    private static Map<String, ? extends Collection<Behavior>> createTestCaseAndIssueIDMap(List<BaseElement> cadidates) {

        HashMap<Behavior, List<String>> map = new HashMap<>();

        for(BaseElement bel : cadidates){
            Behavior el = (Behavior) bel;
            Collection<DirectedRelationship> targets = el.get_directedRelationshipOfSource();
            List<String> reqIDs = new ArrayList<>();
            for (DirectedRelationship relationship : targets)
            {
                if(StereotypesHelper.hasStereotype(relationship, StereotypeUtils.getStereotypeObj(profileSysML, "Verify"))){
                    Collection<Element> target = relationship.getTarget();
                    if(target != null
                            && !target.isEmpty()
                            && StereotypesHelper.hasStereotypeOrDerived(target.iterator().next(),StereotypeUtils.getStereotypeObj(profileSysML, "Requirement"))){
                        List<String> issueIDs = StereotypesHelper.getStereotypePropertyValueAsString(target.iterator().next(), StereotypeUtils.getStereotypeObj(profileMed, "Related Issue"), "issueID");
                        if (issueIDs.size()>0 && !reqIDs.contains(issueIDs.iterator().next())) reqIDs.add(issueIDs.iterator().next());
                    }
                }
            }
            map.put(el, reqIDs);
        }
        return reverse(map);
    }

    public static <K, V> Map<V, ? extends Collection<K>> reverse(Map<K, ? extends Collection<V>> map) {
        final Map<V, Collection<K>> reversedMap = new HashMap<>();
        map.forEach((key, values) -> {
            values.forEach(value -> {
                if (!reversedMap.containsKey(value))
                    reversedMap.put(value, new ArrayList<>());
                final Collection<K> collection = reversedMap.get(value);
                if (!collection.contains(key))
                    reversedMap.get(value).add(key);
            });
        });
       return reversedMap;
    }

    public static HashMap getTestCaseResults(Behavior testCase) {

        Behavior tc = testCase;
        Collection<DirectedRelationship> r = tc.get_directedRelationshipOfSource();
        HashMap map = new HashMap();

        r.forEach(directedRelationship -> {
            if (StereotypesHelper.hasStereotypeOrDerived(directedRelationship, StereotypeUtils.getStereotypeObj(profileMed, "Pass"))) {
                map.put(directedRelationship, directedRelationship.getTarget().iterator().next().getHumanName());
            } else if (StereotypesHelper.hasStereotypeOrDerived(directedRelationship, StereotypeUtils.getStereotypeObj(profileMed, "Fail"))) {
                map.put(directedRelationship, directedRelationship.getTarget().iterator().next().getHumanName());
            }
        });
        return map;
    }

    public void actionPerformed(ActionEvent e) {
        List<java.lang.Class> types = ClassTypes.getSubtypes(Class.class);

        ElementSelectionDlg elementSelectionDlg = createStereotypedElementsSelectionDialog(types, StereotypeUtils.getStereotypeObj("Softneta Medical Profile", "TC"));
        elementSelectionDlg.setVisible(true);

        if (elementSelectionDlg.isOkClicked()) {
            List<BaseElement> selectedElements = elementSelectionDlg.getSelectedElements();
            if (selectedElements.size() > 0) {
                Map<String, ? extends Collection<Behavior>> map = createTestCaseAndIssueIDMap(selectedElements);
                if (!map.isEmpty()) RedmineIssueManager.updateIssueTestReport((HashMap) map);
            }
        }
    }
}
