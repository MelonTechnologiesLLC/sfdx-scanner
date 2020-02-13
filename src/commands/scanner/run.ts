import {flags, SfdxCommand} from '@salesforce/command';
import {Messages, SfdxError} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {OUTPUT_FORMAT, RULE_FILTER_TYPE, RuleFilter, RuleManager} from "../../lib/RuleManager";
import fs = require('fs');

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'run');

export default class Run extends SfdxCommand {
  // These determine what's displayed when the --help/-h flag is provided.
  public static description = messages.getMessage('commandDescription');
  // TODO: Write real examples.
  public static examples = [
    `$ sfdx hello:org --targetusername myOrg@example.com --targetdevhubusername devhub@org.com
  Hello world! This is org: MyOrg and I will be around until Tue Mar 20 2018!
  My hub org id is: 00Dxx000000001234
  `];

  public static args = [{name: 'file'}];

  // This defines the flags accepted by this command.
  protected static flagsConfig = {
    // These flags are how you choose which rules you're running.
    rulename: flags.string({
      char: 'n',
      description: messages.getMessage('flags.rulenameDescription'),
      // If you're specifying by name, it doesn't make sense to let you specify by any other means.
      exclusive: ['category', 'ruleset', 'severity', 'exclude-rule']
    }),
    category: flags.array({
      char: 'c',
      description: messages.getMessage('flags.categoryDescription')
    }),
    ruleset: flags.array({
      char: 'r',
      description: messages.getMessage('flags.rulesetDescription')
    }),
    // TODO: IMPLEMENT THESE FLAGS IN A MEANINGFUL WAY.
    /*
    severity: flags.string({
      char: 's',
      description: messages.getMessage('flags.severityDescription')
    }),
    'exclude-rule': flags.array({
      description: messages.getMessage('flags.excluderuleDescription')
    }),
     */
    // These flags are how you choose which files you're targeting.
    source: flags.array({
      char: 's',
      description: messages.getMessage('flags.sourceDescription'),
      // If you're specifying local files, it doesn't make much sense to let you specify anything else.
      exclusive: ['org']
    }),
    org: flags.string({
      char: 'a',
      description: messages.getMessage('flags.orgDescription'),
      // If you're specifying an org, it doesn't make sense to let you specify anything else.
      exclusive: ['source']
    }),
    // These flags modify how the process runs, rather than what it consumes.
    'suppress-warnings': flags.boolean({
      description: messages.getMessage('flags.suppresswarningsDescription')
    }),
    format: flags.enum({
      char: 'f',
      description: messages.getMessage('flags.formatDescription'),
      options: [OUTPUT_FORMAT.XML, OUTPUT_FORMAT.CSV],
      exclusive: ['outfile']
    }),
    outfile: flags.string({
      char: 'o',
      description: messages.getMessage('flags.outfileDescription'),
      exclusive: ['format']
    })
  };

  public async run(): Promise<AnyJson> {
    // First, we need to do some input validation that's a bit too sophisticated for the out-of-the-box flag validations.
    this.validateFlags();

    // Next, we need to build our input.
    const filters = this.buildRuleFilters();
    const source : string[]|string = this.flags.source || this.flags.org;
    const format : OUTPUT_FORMAT = this.flags.format || this.deriveFormatFromOutfile();
    const ruleManager = new RuleManager();
    // It's possible for this line to throw an error, but that's fine because the error will be an SfdxError that we can
    // allow to boil over.
    const output = await ruleManager.runRulesMatchingCriteria(filters, source, format);
    this.processOutput(output);
    return {};
  }

  private buildRuleFilters() : RuleFilter[] {
    let filters : RuleFilter[] = [];
    // Create a filter for any provided categories.
    if ((this.flags.category || []).length > 0) {
      filters.push(new RuleFilter(RULE_FILTER_TYPE.CATEGORY, this.flags.category));
    }

    // Create a filter for any provided rulesets.
    if ((this.flags.ruleset || []).length > 0) {
      filters.push(new RuleFilter(RULE_FILTER_TYPE.RULESET, this.flags.ruleset));
    }

    // Create a filter for any provided languages.
    if ((this.flags.language || []).length > 0) {
      filters.push(new RuleFilter(RULE_FILTER_TYPE.LANGUAGE, this.flags.language));
    }

    return filters;
  }

  private validateFlags() : void {
    // --source and --org are mutually exclusive, but they can't both be null.
    if (!this.flags.source && !this.flags.org) {
      throw new SfdxError(messages.getMessage('validations.mustTargetSomething'));
    }
    // --format and --outfile are mutually exclusive, but they can't both be null.
    if (!this.flags.format && !this.flags.outfile) {
      throw new SfdxError(messages.getMessage('validations.mustSpecifyOutput'));
    }
  }

  private deriveFormatFromOutfile() : OUTPUT_FORMAT {
    const outfile = this.flags.outfile;
    const lastPeriod = outfile.lastIndexOf('.');
    if (lastPeriod < 1 || lastPeriod + 1 == outfile.length) {
      throw new SfdxError(messages.getMessage('validations.outfileMustBeValid'));
    } else {
      const fileExtension = outfile.slice(lastPeriod + 1);
      switch (fileExtension) {
        case OUTPUT_FORMAT.CSV:
          return OUTPUT_FORMAT.CSV;
        case OUTPUT_FORMAT.XML:
          return OUTPUT_FORMAT.XML;
        default:
          throw new SfdxError(messages.getMessage('validations.outfileMustBeSupportedType'));
      }
    }
  }

  private processOutput(output : string) : void {
    // If the output is an empty string, it means no violations were found, and we should log that information to the console
    // so the user doesn't get confused.
    if (output === '') {
      this.ux.log(messages.getMessage('output.noViolationsDetected'));
    }
    if (this.flags.outfile) {
      // If we were given a file, we should write the output to that file.
      try {
        fs.writeFileSync(this.flags.outfile, output);
      } catch (e) {
        throw new SfdxError(e.message || e);
      }
    } else {
      // If we're just supposed to dump the output to the console, what precisely we do depends on the format.
      if (this.flags.format === OUTPUT_FORMAT.CSV) {
        // TODO: GET THIS PART WORKING
        /*
        // The CSV is one giant string, so we'll split it by newline characters to get the rows, then split those by commas.
        const rowStrings = output.split('\n');
        const columns = rowStrings[0].split('","');
        const rows = [];
        // Initialize the loop at index 1 so we skip the columns.
        for (let i = 1; i < rowStrings.length; i++) {
          rows.push(rowStrings[i].split(','));
        }
        this.ux.table(rows, columns);
         */
        throw new SfdxError('Cannot log CSV to console yet.');
      } else if (this.flags.format === OUTPUT_FORMAT.XML) {
        // For XML, we can just dump it to the console.
        this.ux.log(output);
      }
    }
  }
}
